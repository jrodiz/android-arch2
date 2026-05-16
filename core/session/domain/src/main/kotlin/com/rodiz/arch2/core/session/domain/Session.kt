package com.rodiz.arch2.core.session.domain

data class Session(
    val userId: String,
    val token: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
) {
    override fun toString(): String =
        "Session(userId=$userId, displayName=$displayName, token=[REDACTED])"
}
