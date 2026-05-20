package com.rodiz.arch2.feature.settings.domain.model

import kotlinx.datetime.Instant

data class AccountDeletion(
    val requestedAt: Instant,
    val hardDeleteAt: Instant,    // requestedAt + 30 days
)
