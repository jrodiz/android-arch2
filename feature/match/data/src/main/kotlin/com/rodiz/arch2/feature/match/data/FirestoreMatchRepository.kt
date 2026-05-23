package com.rodiz.arch2.feature.match.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreMatchRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MatchRepository {

    private val matchesCol get() = firestore.collection("matches")

    override fun observeInbox(): Flow<List<Match>> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = matchesCol
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val matches = snap?.documents.orEmpty()
                    .mapNotNull { it.toMatchOrNull() }
                    // sort by max(createdAt, lastMessageAt) desc, in-memory
                    .sortedByDescending { match ->
                        val sortKey = match.lastMessageAt ?: match.createdAt
                        sortKey
                    }
                trySend(matches)
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override fun observeMatch(id: MatchId): Flow<Match?> = callbackFlow {
        val registration = matchesCol.document(id.value).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toMatchOrNull())
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun unmatch(id: MatchId) {
        withContext(io) {
            // Subcollection cleanup is handled by an `onDocumentDeleted` Cloud Function
            // (per notifications-fcm.md). Client-side delete is shallow.
            matchesCol.document(id.value).delete().await()
        }
    }
}

internal fun DocumentSnapshot.toMatchOrNull(): Match? {
    if (!exists()) return null
    return try {
        val ownerA = getString("ownerAId") ?: return null
        val ownerB = getString("ownerBId") ?: return null
        val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: return null
        Match(
            id = MatchId(id),
            ownerAId = ownerA,
            ownerBId = ownerB,
            createdAt = createdAt,
            lastMessageAt = getTimestamp("lastMessageAt")?.toKxInstant(),
            lastMessagePreview = getString("lastMessagePreview"),
            lastMessageFromOwnerId = getString("lastMessageFromOwnerId"),
            petAId = getString("petAId"),
            petBId = getString("petBId"),
        )
    } catch (_: Throwable) {
        null
    }
}

internal fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
