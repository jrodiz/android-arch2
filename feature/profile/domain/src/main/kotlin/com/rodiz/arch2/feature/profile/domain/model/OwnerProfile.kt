package com.rodiz.arch2.feature.profile.domain.model

import kotlinx.datetime.Instant

data class OwnerProfile(
    val id: String,
    val firstName: String,
    val avatarUrl: String?,
    val email: String?,
    val paused: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
