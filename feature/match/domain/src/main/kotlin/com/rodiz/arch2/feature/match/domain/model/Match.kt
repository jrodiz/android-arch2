package com.rodiz.arch2.feature.match.domain.model

import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import kotlinx.datetime.Instant

@JvmInline
value class MatchId(val value: String)

data class Match(
    val id: MatchId,
    val ownerAId: String,
    val ownerBId: String,
    val createdAt: Instant,
    val lastMessageAt: Instant?,
    val lastMessagePreview: String?,
    val lastMessageFromOwnerId: String?,
) {
    fun otherOwnerId(me: String): String = if (ownerAId == me) ownerBId else ownerAId
    val hasMessages: Boolean get() = lastMessageAt != null
}

/** What the Matches inbox row shows. `other` is null while the owner doc is loading. */
data class MatchSummary(
    val match: Match,
    val otherOwnerId: String,
    val other: OwnerDisplay? = null,
)

data class InboxSnapshot(
    val newMatches: List<MatchSummary>,
    val conversations: List<MatchSummary>,
)
