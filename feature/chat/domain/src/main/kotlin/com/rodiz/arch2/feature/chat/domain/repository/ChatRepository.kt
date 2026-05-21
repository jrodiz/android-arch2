package com.rodiz.arch2.feature.chat.domain.repository

import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.match.domain.model.MatchId
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Continuous stream of messages for this chat (oldest first), capped at [pageSize]. */
    fun observeChat(matchId: MatchId, pageSize: Int = 50): Flow<List<Message>>

    /** Send a text message. Trims input; throws on empty or > 2000 chars. */
    suspend fun sendMessage(matchId: MatchId, text: String): Message

    /** Mark all currently-visible incoming messages as read by the current user. */
    suspend fun markAllRead(matchId: MatchId)

    /**
     * Block the other participant of this match. Writes /blocks/{me_other} and
     * deletes the match doc (Cloud Function recursively cleans up messages).
     */
    suspend fun blockOther(matchId: MatchId)
}
