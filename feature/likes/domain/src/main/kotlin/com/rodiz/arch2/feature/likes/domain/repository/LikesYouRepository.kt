package com.rodiz.arch2.feature.likes.domain.repository

import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import kotlinx.coroutines.flow.Flow

interface LikesYouRepository {
    /** Likes targeting one of my pets, minus passedLikes; newest first. */
    fun observeLikesYou(): Flow<List<IncomingLike>>

    /** Hide this like from the grid. Doesn't affect the underlying likes doc; doesn't prevent reciprocity. */
    suspend fun pass(key: LikeKey)

    /** Delegate to the deck's submitSwipe(LIKE) on the liker's anchor pet — produces an instant match. */
    suspend fun likeBack(key: LikeKey): SwipeResult
}
