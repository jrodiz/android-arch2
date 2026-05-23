package com.rodiz.arch2.feature.settings.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs
import com.rodiz.arch2.feature.settings.domain.repository.NotificationPrefsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreNotificationPrefsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepo: SessionRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : NotificationPrefsRepository {

    private val ownersCol get() = firestore.collection("owners")

    override fun observePrefs(): Flow<NotificationPrefs> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(NotificationPrefs.DEFAULT)
            close()
            return@callbackFlow
        }
        val registration = ownersCol.document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val map = snap?.get("notifications") as? Map<String, Any?>
            trySend(map.toPrefsOrDefault())
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun updatePrefs(prefs: NotificationPrefs) {
        withContext(io) {
            val uid = sessionRepo.current()?.userId ?: error("No signed-in user")
            ownersCol.document(uid).set(
                mapOf("notifications" to prefs.toMap()),
                SetOptions.merge(),
            ).await()
        }
    }
}

private fun NotificationPrefs.toMap(): Map<String, Any> = mapOf(
    "newMatch" to newMatch,
    "newMessage" to newMessage,
    "someoneLiked" to someoneLiked,
    "weeklyDigest" to weeklyDigest,
    "quietHoursEnabled" to quietHoursEnabled,
    "quietHoursStartMinutes" to quietHoursStartMinutes,
    "quietHoursEndMinutes" to quietHoursEndMinutes,
)

private fun Map<String, Any?>?.toPrefsOrDefault(): NotificationPrefs {
    if (this == null) return NotificationPrefs.DEFAULT
    val d = NotificationPrefs.DEFAULT
    return NotificationPrefs(
        newMatch = this["newMatch"] as? Boolean ?: d.newMatch,
        newMessage = this["newMessage"] as? Boolean ?: d.newMessage,
        someoneLiked = this["someoneLiked"] as? Boolean ?: d.someoneLiked,
        weeklyDigest = this["weeklyDigest"] as? Boolean ?: d.weeklyDigest,
        quietHoursEnabled = this["quietHoursEnabled"] as? Boolean ?: d.quietHoursEnabled,
        // Firestore stores ints as Long; read via Number so both round-trip safely.
        quietHoursStartMinutes = (this["quietHoursStartMinutes"] as? Number)?.toInt()
            ?: d.quietHoursStartMinutes,
        quietHoursEndMinutes = (this["quietHoursEndMinutes"] as? Number)?.toInt()
            ?: d.quietHoursEndMinutes,
    )
}
