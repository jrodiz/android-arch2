package com.rodiz.arch2.feature.match.domain.usecase

import com.rodiz.arch2.feature.match.domain.model.InboxSnapshot
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.model.MatchSummary
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveInboxUseCase @Inject constructor(
    private val repo: MatchRepository,
) {
    /** [currentUid] is needed to compute otherOwnerId per row. */
    operator fun invoke(currentUid: String): Flow<InboxSnapshot> =
        repo.observeInbox().map { matches ->
            val summaries = matches.map { MatchSummary(it, it.otherOwnerId(currentUid)) }
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
