package com.rodiz.arch2.core.featuredpets.data

import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge between the session observer (App.kt) and the featured-pets cache.
 *
 * - Sign-out (`userId == null`) is intentionally a no-op so the Login hero
 *   can keep showing the previous user's pets.
 * - A different `userId` triggers a [FeaturedPetsRepository.wipe] so the
 *   newly-signed-in user starts with a clean selection.
 */
@Singleton
class FeaturedPetsSessionGate @Inject constructor(
    private val repo: FeaturedPetsRepository,
) {
    suspend fun onSessionChanged(userId: String?) {
        if (userId == null) return
        when (repo.onUserActive(userId)) {
            UserChangeResult.USER_CHANGED -> repo.wipe()
            UserChangeResult.FIRST_USER, UserChangeResult.SAME_USER -> Unit
        }
    }
}
