package com.rodiz.arch2.feature.match.domain.model

import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.petlookup.domain.PetDisplay
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
    /**
     * Pet on the A side. Null for matches created before the Cloud Function started
     * writing this field (see plans/match-inbox-redesign.md follow-up); the UI degrades
     * to owner-name-only when null.
     */
    val petAId: String? = null,
    /** Pet on the B side. Same nullability story as [petAId]. */
    val petBId: String? = null,
) {
    fun otherOwnerId(me: String): String = if (ownerAId == me) ownerBId else ownerAId
    fun otherPetId(me: String): String? = if (ownerAId == me) petBId else petAId
    val hasMessages: Boolean get() = lastMessageAt != null
}

/**
 * What the Matches inbox row shows. `other` is null while the owner doc is loading;
 * `otherPet` is null when the match predates pet plumbing or while the pet doc loads.
 */
data class MatchSummary(
    val match: Match,
    val otherOwnerId: String,
    val other: OwnerDisplay? = null,
    val otherPet: PetDisplay? = null,
)

data class InboxSnapshot(
    val newMatches: List<MatchSummary>,
    val conversations: List<MatchSummary>,
)
