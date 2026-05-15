package com.rodiz.arch2.core.session.domain

data class Session(
    val userId: String,
    val token: String,
) {
    override fun toString(): String = "Session(userId=$userId, token=[REDACTED])"
}
