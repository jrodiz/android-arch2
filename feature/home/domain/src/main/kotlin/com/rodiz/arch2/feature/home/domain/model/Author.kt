package com.rodiz.arch2.feature.home.domain.model

@JvmInline
value class AuthorId(val value: String)

data class Author(
    val id: AuthorId,
    val displayName: String,
    val avatarUrl: String?,
)
