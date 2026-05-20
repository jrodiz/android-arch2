package com.rodiz.arch2.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.chat.domain.model.MessageId
import com.rodiz.arch2.feature.chat.domain.repository.ChatRepository
import com.rodiz.arch2.feature.match.domain.model.MatchId
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
internal class FirestoreChatRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ChatRepository {

    private val matchesCol get() = firestore.collection("matches")

    override fun observeChat(matchId: MatchId, pageSize: Int): Flow<List<Message>> = callbackFlow {
        val registration = matchesCol.document(matchId.value)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(pageSize.toLong())
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val messages = snap?.documents.orEmpty().mapNotNull { it.toMessageOrNull(matchId) }
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun sendMessage(matchId: MatchId, text: String): Message = withContext(io) {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Message cannot be blank" }
        require(trimmed.length <= 2000) { "Message exceeds 2000 chars" }

        val me = sessionRepo.current()?.userId ?: error("No signed-in user")
        val matchRef = matchesCol.document(matchId.value)
        val msgRef = matchRef.collection("messages").document()

        firestore.runBatch { batch ->
            batch.set(msgRef, mapOf(
                "fromOwnerId" to me,
                "text" to trimmed,
                "createdAt" to FieldValue.serverTimestamp(),
                "readBy" to mapOf(me to FieldValue.serverTimestamp()),
            ))
            batch.update(matchRef, mapOf(
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastMessagePreview" to trimmed.take(60),
                "lastMessageFromOwnerId" to me,
            ))
        }.await()

        // Return a locally-constructed Message; the snapshot listener will emit the canonical version too.
        val nowSec = System.currentTimeMillis() / 1000
        Message(
            id = MessageId(msgRef.id),
            matchId = matchId,
            fromOwnerId = me,
            text = trimmed,
            createdAt = Instant.fromEpochSeconds(nowSec),
            readBy = mapOf(me to Instant.fromEpochSeconds(nowSec)),
        )
    }

    override suspend fun markAllRead(matchId: MatchId) {
        withContext(io) {
            val me = sessionRepo.current()?.userId ?: return@withContext
            // Fetch messages not authored by me and not already read by me, in the latest page.
            val msgs = matchesCol.document(matchId.value)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            val batch = firestore.batch()
            msgs.documents.forEach { doc ->
                val from = doc.getString("fromOwnerId") ?: return@forEach
                if (from == me) return@forEach
                @Suppress("UNCHECKED_CAST")
                val readBy = (doc.get("readBy") as? Map<String, Any?>).orEmpty()
                if (me in readBy) return@forEach
                batch.update(doc.reference, "readBy.$me", FieldValue.serverTimestamp())
            }
            batch.commit().await()
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toMessageOrNull(matchId: MatchId): Message? {
    if (!exists()) return null
    return try {
        val from = getString("fromOwnerId") ?: return null
        val text = getString("text") ?: return null
        val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: return null
        val readByRaw = (get("readBy") as? Map<String, Any?>).orEmpty()
        val readBy = readByRaw.mapNotNull { (uid, value) ->
            (value as? Timestamp)?.let { uid to it.toKxInstant() }
        }.toMap()
        Message(
            id = MessageId(id),
            matchId = matchId,
            fromOwnerId = from,
            text = text,
            createdAt = createdAt,
            readBy = readBy,
        )
    } catch (_: Throwable) {
        null
    }
}

internal fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
