package com.rodiz.arch2.feature.home.domain.repository

import com.rodiz.arch2.feature.home.domain.model.Post
import com.rodiz.arch2.feature.home.domain.model.PostId
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeFeed(): Flow<List<Post>>

    suspend fun refresh()

    suspend fun toggleLike(postId: PostId)
}
