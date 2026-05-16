package com.rodiz.arch2.feature.home.presentation

import com.rodiz.arch2.feature.home.domain.model.PostId

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val posts: List<PostUiModel>,
        val isRefreshing: Boolean,
    ) : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

data class PostUiModel(
    val id: PostId,
    val authorName: String,
    val authorAvatarUrl: String?,
    val authorInitial: String,
    val relativeTime: String,
    val text: String?,
    val imageUrl: String?,
    val likeCount: Int,
    val formattedLikeCount: String,
    val commentCount: Int,
    val formattedCommentCount: String,
    val shareCount: Int,
    val formattedShareCount: String,
    val viewerHasLiked: Boolean,
)
