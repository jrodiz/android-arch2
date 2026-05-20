package com.rodiz.arch2.feature.likes.domain.usecase

import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import com.rodiz.arch2.feature.likes.domain.repository.LikesYouRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLikesYouUseCase @Inject constructor(
    private val repo: LikesYouRepository,
) {
    operator fun invoke(): Flow<List<IncomingLike>> = repo.observeLikesYou()
}

class PassLikeUseCase @Inject constructor(
    private val repo: LikesYouRepository,
) {
    suspend operator fun invoke(key: LikeKey) = repo.pass(key)
}

class LikeBackUseCase @Inject constructor(
    private val repo: LikesYouRepository,
) {
    suspend operator fun invoke(key: LikeKey): SwipeResult = repo.likeBack(key)
}
