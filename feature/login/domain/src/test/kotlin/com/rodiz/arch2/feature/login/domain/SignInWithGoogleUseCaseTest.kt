package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignInWithGoogleUseCaseTest {

    private val idToken = "fake.google.id.token"

    @Test
    fun `success result is propagated`() = runTest {
        val session = Session(
            userId = "google-uid",
            token = "fb-id-token",
            displayName = "Joe Demo",
            photoUrl = "https://example/photo.jpg",
        )
        val repo = FakeAuthRepo(googleResult = Try.Success(session))

        val result = SignInWithGoogleUseCase(repo).invoke(idToken)

        assertEquals(Try.Success(session), result)
    }

    @Test
    fun `each AuthError from Google flow is propagated unchanged`() = runTest {
        val errors: List<AuthError> = listOf(
            AuthError.GoogleSignInCancelled,
            AuthError.GoogleSignInFailed,
            AuthError.NoNetwork,
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(googleResult = Try.Failure(error))
            val result = SignInWithGoogleUseCase(repo).invoke(idToken)
            assertEquals(Try.Failure(error), result, "AuthError ${'$'}error should propagate")
        }
    }

    private class FakeAuthRepo(
        val googleResult: Try<Session, AuthError>,
    ) : AuthRepository {
        override suspend fun login(credentials: Credentials) = error("not used")
        override suspend fun loginWithStoredCredentials() = error("not used")
        override suspend fun signInWithGoogle(idToken: String) = googleResult
        override suspend fun hasStoredCredentials() = false
    }
}
