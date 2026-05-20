package com.rodiz.arch2.feature.match.domain.repository

import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    /** All matches for the current owner, sorted by max(createdAt, lastMessageAt) desc. */
    fun observeInbox(): Flow<List<Match>>

    /** Single match by id; null after deletion. */
    fun observeMatch(id: MatchId): Flow<Match?>

    /** Deletes the match record. A Cloud Function (notifications-fcm.md) handles subcollection cleanup. */
    suspend fun unmatch(id: MatchId)
}
