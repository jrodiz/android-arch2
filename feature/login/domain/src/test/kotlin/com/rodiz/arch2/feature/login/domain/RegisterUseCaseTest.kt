package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.model.SignUpRequest
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.RegisterUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterUseCaseTest {

    private val request = SignUpRequest(
        firstName = "Steve",
        lastName = "Rogers",
        email = "steve@example.com",
        password = "password1",
    )

    @Test
    fun `success result is propagated`() = runTest {
        val session = Session(userId = "u1", token = "t1", displayName = "Steve Rogers")
        val repo = FakeAuthRepo(registerResult = Try.Success(session))

        val result = RegisterUseCase(repo).invoke(request)

        assertEquals(Try.Success(session), result)
    }

    @Test
    fun `each AuthError from register flow is propagated unchanged`() = runTest {
        val errors: List<AuthError> = listOf(
            AuthError.EmailAlreadyInUse,
            AuthError.WeakPassword,
            AuthError.AvatarUploadFailed,
            AuthError.NoNetwork,
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(registerResult = Try.Failure(error))
            val result = RegisterUseCase(repo).invoke(request)
            assertEquals(Try.Failure(error), result, "AuthError ${'$'}error should propagate")
        }
    }

    private class FakeAuthRepo(
        val registerResult: Try<Session, AuthError>,
    ) : AuthRepository {
        override suspend fun login(credentials: Credentials) = error("not used")
        override suspend fun loginWithStoredCredentials() = error("not used")
        override suspend fun signInWithGoogle(idToken: String) = error("not used")
        override suspend fun register(request: SignUpRequest) = registerResult
        override suspend fun hasStoredCredentials() = false
    }
}
