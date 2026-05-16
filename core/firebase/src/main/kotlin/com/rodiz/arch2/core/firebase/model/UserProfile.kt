package com.rodiz.arch2.core.firebase.model

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: String,
)
