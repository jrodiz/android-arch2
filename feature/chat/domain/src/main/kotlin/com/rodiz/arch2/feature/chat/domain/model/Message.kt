package com.rodiz.arch2.feature.chat.domain.model

import com.rodiz.arch2.feature.match.domain.model.MatchId
import kotlinx.datetime.Instant

@JvmInline
value class MessageId(val value: String)

data class Message(
    val id: MessageId,
    val matchId: MatchId,
    val fromOwnerId: String,
    val text: String,
    val createdAt: Instant,
    val readBy: Map<String, Instant>,
) {
    fun isReadBy(otherUid: String): Boolean = readBy.containsKey(otherUid)
}
