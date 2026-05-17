package com.rodiz.arch2.feature.login.domain.model

data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val avatarUri: String? = null,
) {
    val displayName: String get() = "${firstName.trim()} ${lastName.trim()}".trim()

    override fun toString(): String =
        "SignUpRequest(firstName=$firstName, lastName=$lastName, email=$email, " +
            "password=[REDACTED], avatarUri=${avatarUri ?: "null"})"
}
