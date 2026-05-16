package com.rodiz.arch2.feature.home.domain.model

import kotlinx.datetime.Instant

@JvmInline
value class PostId(val value: String)

data class Post(
    val id: PostId,
    val author: Author,
    val createdAt: Instant,
    val text: String?,
    val imageUrl: String?,
    val reactions: Reactions,
    val commentCount: Int,
    val shareCount: Int,
    val viewerHasLiked: Boolean,
) {
    init {
        require(!text.isNullOrBlank() || !imageUrl.isNullOrBlank()) {
            "Post must have text or imageUrl"
        }
    }
}
