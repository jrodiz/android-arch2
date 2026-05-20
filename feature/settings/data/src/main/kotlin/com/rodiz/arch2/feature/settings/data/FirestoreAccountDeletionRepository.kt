package com.rodiz.arch2.feature.settings.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreAccountDeletionRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AccountDeletionRepository {

    private val deletionsCol get() = firestore.collection("accountDeletions")

    override fun observePendingDeletion(): Flow<AccountDeletion?> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = deletionsCol.document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val requested = snap?.getTimestamp("requestedAt")?.toKxInstant()
            val hardDelete = snap?.getTimestamp("hardDeleteAt")?.toKxInstant()
            val deletion = if (requested != null && hardDelete != null) {
                AccountDeletion(requestedAt = requested, hardDeleteAt = hardDelete)
            } else {
                null
            }
            trySend(deletion)
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun requestDeletion(): AccountDeletion = withContext(io) {
        val uid = sessionRepo.current()?.userId ?: error("No signed-in user")
        val now = Clock.System.now()
        val hardDelete = Instant.fromEpochSeconds(now.epochSeconds + DAYS_30_SECONDS)
        deletionsCol.document(uid).set(
            mapOf(
                "requestedAt" to Timestamp(Date.from(java.time.Instant.ofEpochSecond(now.epochSeconds))),
                "hardDeleteAt" to Timestamp(Date.from(java.time.Instant.ofEpochSecond(hardDelete.epochSeconds))),
            ),
        ).await()
        AccountDeletion(requestedAt = now, hardDeleteAt = hardDelete)
    }

    override suspend fun cancelDeletion() {
        withContext(io) {
            val uid = sessionRepo.current()?.userId ?: return@withContext
            deletionsCol.document(uid).delete().await()
        }
    }

    companion object {
        private const val DAYS_30_SECONDS: Long = 30L * 24L * 60L * 60L
    }
}

internal fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
