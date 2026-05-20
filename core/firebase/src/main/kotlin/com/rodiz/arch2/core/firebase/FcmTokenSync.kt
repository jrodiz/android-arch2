package com.rodiz.arch2.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the current device's FCM token to fcmTokens/{sha256(token).take(40)} so a Cloud
 * Function can read it later to fan out push deliveries. Idempotent — re-syncing the same
 * token just overwrites the doc with the same values.
 */
@Singleton
class FcmTokenSync @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun syncForSignedInUser() = withContext(io) {
        val uid = sessionRepo.current()?.userId ?: return@withContext
        val token = messaging.token.await()
        val hash = sha256(token).take(40)
        firestore.collection("fcmTokens").document(hash).set(
            mapOf(
                "token" to token,
                "platform" to "android",
                "ownerId" to uid,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
            ),
        ).await()
    }

    suspend fun clearForSignOut(token: String) = withContext(io) {
        val hash = sha256(token).take(40)
        runCatching { firestore.collection("fcmTokens").document(hash).delete().await() }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
