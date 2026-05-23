package com.rodiz.arch2.feature.match.domain.usecase

import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import com.rodiz.arch2.feature.match.domain.model.InboxSnapshot
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.model.MatchSummary
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveInboxUseCase @Inject constructor(
    private val repo: MatchRepository,
    private val ownerLookup: OwnerLookupRepository,
    private val petLookup: PetLookupRepository,
) {
    /** [currentUid] is needed to compute otherOwnerId per row. */
    operator fun invoke(currentUid: String): Flow<InboxSnapshot> =
        combine(
            repo.observeInbox(),
            ownerLookup.observeAll(),
            petLookup.observeAll(),
        ) { matches, owners, pets ->
            val summaries = matches.map { match ->
                val otherId = match.otherOwnerId(currentUid)
                val otherPetId = match.otherPetId(currentUid)
                MatchSummary(
                    match = match,
                    otherOwnerId = otherId,
                    other = owners[otherId],
                    otherPet = otherPetId?.let { pets[it] },
                )
            }
            InboxSnapshot(
                newMatches = summaries.filterNot { it.match.hasMessages },
                conversations = summaries.filter { it.match.hasMessages },
            )
        }
}

class ObserveMatchUseCase @Inject constructor(
    private val repo: MatchRepository,
) {
    operator fun invoke(id: MatchId): Flow<Match?> = repo.observeMatch(id)
}

class UnmatchUseCase @Inject constructor(
    private val repo: MatchRepository,
) {
    suspend operator fun invoke(id: MatchId) = repo.unmatch(id)
}
