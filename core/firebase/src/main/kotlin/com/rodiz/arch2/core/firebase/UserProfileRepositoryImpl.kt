package com.rodiz.arch2.core.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rodiz.arch2.core.firebase.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserProfileRepository {

    override suspend fun upsertOnSignIn(profile: UserProfile) {
        val doc = firestore.collection(COLLECTION).document(profile.uid)
        val base: Map<String, Any?> = mapOf(
            "uid" to profile.uid,
            "email" to profile.email,
            "displayName" to profile.displayName,
            "photoUrl" to profile.photoUrl,
            "provider" to profile.provider,
            "lastSignInAt" to FieldValue.serverTimestamp(),
        )
        // createdAt only on first write — set with serverTimestamp + merge so a
        // pre-existing value isn't overwritten on subsequent sign-ins.
        val withCreatedAt = base + ("createdAt" to FieldValue.serverTimestamp())
        val existing = doc.get().await()
        val payload = if (existing.exists()) base else withCreatedAt
        doc.set(payload, SetOptions.merge()).await()
    }

    override fun observe(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toProfile())
            }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProfile(): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            uid = getString("uid") ?: id,
            email = getString("email"),
            displayName = getString("displayName"),
            photoUrl = getString("photoUrl"),
            provider = getString("provider").orEmpty(),
        )
    }

    private companion object {
        const val COLLECTION = "users"
    }
}
