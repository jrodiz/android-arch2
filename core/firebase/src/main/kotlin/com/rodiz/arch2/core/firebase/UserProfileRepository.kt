package com.rodiz.arch2.core.firebase

import com.rodiz.arch2.core.firebase.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    suspend fun upsertOnSignIn(profile: UserProfile)
    fun observe(uid: String): Flow<UserProfile?>
}
