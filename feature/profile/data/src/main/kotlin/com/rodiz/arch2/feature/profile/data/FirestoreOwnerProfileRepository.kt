package com.rodiz.arch2.feature.profile.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.profile.domain.repository.OwnerProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreOwnerProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sessionRepo: SessionRepository,
    private val avatarUploader: AvatarUploader,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OwnerProfileRepository {

    private val ownersCol get() = firestore.collection("owners")

    override fun observeMyProfile(): Flow<OwnerProfile?> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = ownersCol.document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            // Seed from Firebase Auth if the Firestore doc hasn't been written yet — sign-up
            // populates displayName/photoUrl on the Auth user before any owners/{uid} write
            // exists, and we want the edit screen to start from those values, not from blanks.
            // Email is always pulled from Auth (it's not stored on the owners doc).
            val email = firebaseAuth.currentUser?.email
            trySend(snap?.toOwnerProfile(uid, email) ?: seedFromAuth(uid))
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun updateFirstName(name: String) {
        withContext(io) {
            val uid = currentUid()
            val now = Clock.System.now()
            ownersCol.document(uid).set(
                mapOf(
                    "firstName" to name,
                    "updatedAt" to now.toTimestamp(),
                    "createdAt" to now.toTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    override suspend fun updateAvatar(localUri: String) {
        withContext(io) {
            val uid = currentUid()
            val url = avatarUploader.upload(uid, Uri.parse(localUri))
            val now = Clock.System.now()
            ownersCol.document(uid).set(
                mapOf(
                    "avatarUrl" to url,
                    "updatedAt" to now.toTimestamp(),
                    "createdAt" to now.toTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    override suspend fun setPaused(paused: Boolean) {
        withContext(io) {
            val uid = currentUid()
            val now = Clock.System.now()
            ownersCol.document(uid).set(
                mapOf(
                    "paused" to paused,
                    "updatedAt" to now.toTimestamp(),
                    "createdAt" to now.toTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    private suspend fun currentUid(): String =
        sessionRepo.current()?.userId ?: error("No signed-in user")

    private fun seedFromAuth(uid: String): OwnerProfile {
        val user = firebaseAuth.currentUser
        val now = Clock.System.now()
        return OwnerProfile(
            id = uid,
            firstName = user?.displayName.orEmpty(),
            avatarUrl = user?.photoUrl?.toString(),
            email = user?.email,
            paused = false,
            createdAt = now,
            updatedAt = now,
        )
    }
}

private fun DocumentSnapshot.toOwnerProfile(uid: String, email: String?): OwnerProfile? {
    if (!exists()) return null
    val firstName = getString("firstName") ?: return null
    val avatarUrl = getString("avatarUrl")
    val paused = getBoolean("paused") ?: false
    val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
    val updatedAt = getTimestamp("updatedAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
    return OwnerProfile(
        id = uid,
        firstName = firstName,
        avatarUrl = avatarUrl,
        email = email,
        paused = paused,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun Instant.toTimestamp(): Timestamp = Timestamp(Date.from(toJavaInstant()))
private fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
