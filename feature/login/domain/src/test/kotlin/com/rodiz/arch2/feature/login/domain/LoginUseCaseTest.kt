package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.LoginUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginUseCaseTest {

    private val credentials = Credentials("user@example.com", "password1")

    @Test
    fun `success result is propagated`() = runTest {
        val session = Session(userId = "u1", token = "t1")
        val repo = FakeAuthRepo(loginResult = Try.Success(session))
        val useCase = LoginUseCase(repo)

        val result = useCase(credentials)

        assertEquals(Try.Success(session), result)
    }

    @Test
    fun `each AuthError is propagated unchanged`() = runTest {
        val errors: List<AuthError> = listOf(
            AuthError.InvalidCredentials,
            AuthError.NoNetwork,
            AuthError.Timeout,
            AuthError.Server(503),
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(loginResult = Try.Failure(error))
            val result = LoginUseCase(repo).invoke(credentials)
            assertEquals(Try.Failure(error), result, "AuthError ${'$'}error should propagate")
        }
    }

    private class FakeAuthRepo(
        val loginResult: Try<Session, AuthError>,
    ) : AuthRepository {
        override suspend fun login(credentials: Credentials) = loginResult
        override suspend fun loginWithStoredCredentials() = loginResult
        override suspend fun signInWithGoogle(idToken: String) = loginResult
        override suspend fun hasStoredCredentials() = false
    }
}
