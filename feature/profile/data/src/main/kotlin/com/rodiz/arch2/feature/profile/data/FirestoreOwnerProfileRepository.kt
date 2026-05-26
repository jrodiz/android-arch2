package com.rodiz.arch2.feature.profile.data

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.profile.domain.model.GeoPoint
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
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

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
            val authUser = firebaseAuth.currentUser
            val displayNameFallback = authUser?.displayName.orEmpty()
            trySend(
                snap?.toOwnerProfile(uid, authUser?.email, displayNameFallback)
                    ?: seedFromAuth(uid),
            )
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

    override suspend fun updateBio(bio: String) {
        withContext(io) {
            val uid = currentUid()
            val now = Clock.System.now()
            ownersCol.document(uid).set(
                mapOf(
                    "bio" to bio,
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
            // Log the second phase separately so a hang on the Firestore write
            // (auth refresh, listen rebuilds, owner doc rules) is distinguishable
            // from a Storage upload hang in logcat.
            Log.i(TAG, "updateAvatar: storage upload OK, writing avatarUrl to Firestore for uid=$uid")
            val now = Clock.System.now()
            try {
                ownersCol.document(uid).set(
                    mapOf(
                        "avatarUrl" to url,
                        "updatedAt" to now.toTimestamp(),
                        "createdAt" to now.toTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
                Log.i(TAG, "updateAvatar: Firestore write OK")
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                Log.e(TAG, "updateAvatar: Firestore write FAILED ${e.javaClass.simpleName}: ${e.message}", e)
                throw e
            }
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

    override suspend fun updateLocation(point: GeoPoint) {
        withContext(io) {
            val uid = currentUid()
            val now = Clock.System.now()
            ownersCol.document(uid).set(
                mapOf(
                    "location" to FirestoreGeoPoint(point.lat, point.lng),
                    "geohash" to point.geohash,
                    "cityLabel" to point.cityLabel,
                    "updatedAt" to now.toTimestamp(),
                    "createdAt" to now.toTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    private suspend fun currentUid(): String =
        sessionRepo.current()?.userId ?: error("No signed-in user")

    private companion object {
        const val TAG = "TinPet.OwnerProfileRepo"
    }

    private fun seedFromAuth(uid: String): OwnerProfile {
        val user = firebaseAuth.currentUser
        val now = Clock.System.now()
        return OwnerProfile(
            id = uid,
            firstName = user?.displayName.orEmpty(),
            avatarUrl = user?.photoUrl?.toString(),
            email = user?.email,
            paused = false,
            location = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}

/**
 * [displayNameFallback] is used for `firstName` when the Firestore doc exists
 * but the field hasn't been written yet (e.g. a fresh user uploaded an avatar
 * before tapping Save on their name). Previously this function returned null
 * on a missing firstName, which silently dropped every other field the doc
 * DID have — including a freshly-uploaded `avatarUrl` — and triggered a
 * fallback to seedFromAuth that wiped the upload from the UI.
 */
private fun DocumentSnapshot.toOwnerProfile(
    uid: String,
    email: String?,
    displayNameFallback: String,
): OwnerProfile? {
    if (!exists()) return null
    val firstName = getString("firstName") ?: displayNameFallback
    val avatarUrl = getString("avatarUrl")
    val paused = getBoolean("paused") ?: false
    val location = (get("location") as? FirestoreGeoPoint)?.let { native ->
        GeoPoint(
            lat = native.latitude,
            lng = native.longitude,
            geohash = getString("geohash").orEmpty(),
            cityLabel = getString("cityLabel"),
        )
    }
    val bio = getString("bio").orEmpty()
    val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
    val updatedAt = getTimestamp("updatedAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
    return OwnerProfile(
        id = uid,
        firstName = firstName,
        avatarUrl = avatarUrl,
        email = email,
        paused = paused,
        location = location,
        createdAt = createdAt,
        updatedAt = updatedAt,
        bio = bio,
    )
}

private fun Instant.toTimestamp(): Timestamp = Timestamp(Date.from(toJavaInstant()))
private fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
