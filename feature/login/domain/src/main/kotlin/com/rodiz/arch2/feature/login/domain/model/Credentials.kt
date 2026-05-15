package com.rodiz.arch2.feature.login.domain.model

data class Credentials(
    val email: String,
    val password: String,
) {
    override fun toString(): String = "Credentials(email=$email, password=[REDACTED])"
}
