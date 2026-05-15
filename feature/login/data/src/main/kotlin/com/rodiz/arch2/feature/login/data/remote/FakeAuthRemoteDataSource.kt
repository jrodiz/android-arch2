package com.rodiz.arch2.feature.login.data.remote

import com.rodiz.arch2.feature.login.data.remote.dto.AuthDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake remote source. Always succeeds after a 1-second delay.
 *
 * Failure-path testing (no network / 401 / 5xx / timeout) is done by injecting a
 * test double in unit tests rather than configuring this class — production
 * code should remain a single happy-path implementation.
 */
@Singleton
internal class FakeAuthRemoteDataSource @Inject constructor() {
    suspend fun login(email: String, @Suppress("UNUSED_PARAMETER") password: String): AuthDto {
        delay(REQUEST_DELAY_MS)
        return AuthDto(
            userId = email.substringBefore('@').ifBlank { "user" },
            token = "fake-token-${System.currentTimeMillis()}",
        )
    }

    companion object {
        const val REQUEST_DELAY_MS = 1_000L
    }
}
