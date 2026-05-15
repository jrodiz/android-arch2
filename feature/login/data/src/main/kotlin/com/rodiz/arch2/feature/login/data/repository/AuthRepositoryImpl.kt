package com.rodiz.arch2.feature.login.data.repository

import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.login.data.local.CredentialVault
import com.rodiz.arch2.feature.login.data.mapper.toSession
import com.rodiz.arch2.feature.login.data.remote.FakeAuthRemoteDataSource
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuthRepositoryImpl @Inject constructor(
    private val remote: FakeAuthRemoteDataSource,
    private val vault: CredentialVault,
    private val sessionRepository: SessionRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(credentials: Credentials): Try<Session, AuthError> =
        withContext(io) {
            performLogin(credentials, persistForBiometric = true)
        }

    override suspend fun loginWithStoredCredentials(): Try<Session, AuthError> = withContext(io) {
        val stored = vault.load() ?: return@withContext Try.Failure(AuthError.InvalidCredentials)
        performLogin(stored, persistForBiometric = false)
    }

    override suspend fun hasStoredCredentials(): Boolean = withContext(io) { vault.exists() }

    private suspend fun performLogin(
        credentials: Credentials,
        persistForBiometric: Boolean,
    ): Try<Session, AuthError> = try {
        val dto = remote.login(credentials.email, credentials.password)
        val session = dto.toSession()
        sessionRepository.save(session)
        if (persistForBiometric) vault.store(credentials)
        Try.Success(session)
    } catch (e: SocketTimeoutException) {
        Try.Failure(AuthError.Timeout)
    } catch (e: TimeoutCancellationException) {
        Try.Failure(AuthError.Timeout)
    } catch (e: IOException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (e: HttpException) {
        when (e.code) {
            401 -> Try.Failure(AuthError.InvalidCredentials)
            in 500..599 -> Try.Failure(AuthError.Server(e.code))
            else -> Try.Failure(AuthError.Unknown)
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.Unknown)
    }
}

/** Lightweight HTTP exception used by fake/real auth sources to signal an HTTP error code. */
internal class HttpException(val code: Int) : RuntimeException("HTTP $code")
