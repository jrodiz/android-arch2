package com.rodiz.arch2.feature.settings.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.settings.domain.model.BlockedOwner
import com.rodiz.arch2.feature.settings.domain.repository.BlockRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreBlockRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepo: SessionRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BlockRepository {

    private val blocksCol get() = firestore.collection("blocks")
    private val ownersCol get() = firestore.collection("owners")

    override fun observeBlockedOwners(): Flow<List<BlockedOwner>> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = blocksCol
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val docs = snap?.documents.orEmpty()
                // Join each block doc with its owners/{otherId} display info.
                // Launch on the producer's CoroutineScope so the lookups cancel with the flow.
                launch {
                    val blocked = docs.mapNotNull { doc ->
                        val otherId = doc.getString("blockedOwnerId") ?: return@mapNotNull null
                        val at = doc.getTimestamp("blockedAt")?.toKxInstantLocal()
                            ?: Clock.System.now()
                        val ownerSnap = runCatching {
                            ownersCol.document(otherId).get().await()
                        }.getOrNull()
                        BlockedOwner(
                            id = otherId,
                            firstName = ownerSnap?.getString("firstName") ?: "Unknown",
                            avatarUrl = ownerSnap?.getString("avatarUrl"),
                            blockedAt = at,
                        )
                    }
                    trySend(blocked)
                }
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun block(otherOwnerId: String) {
        withContext(io) {
            val me = sessionRepo.current()?.userId ?: error("No signed-in user")
            val blockId = "${me}_$otherOwnerId"
            blocksCol.document(blockId).set(
                mapOf(
                    "ownerId" to me,
                    "blockedOwnerId" to otherOwnerId,
                    "blockedAt" to Timestamp(Date.from(java.time.Instant.now())),
                ),
            ).await()
        }
    }

    override suspend fun unblock(otherOwnerId: String) {
        withContext(io) {
            val me = sessionRepo.current()?.userId ?: return@withContext
            val blockId = "${me}_$otherOwnerId"
            blocksCol.document(blockId).delete().await()
        }
    }
}

private fun Timestamp.toKxInstantLocal(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
