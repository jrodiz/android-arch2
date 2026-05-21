package com.rodiz.arch2.feature.settings.domain.model

import kotlinx.datetime.Instant

data class BlockedOwner(
    val id: String,
    val firstName: String,
    val avatarUrl: String?,
    val blockedAt: Instant,
)
