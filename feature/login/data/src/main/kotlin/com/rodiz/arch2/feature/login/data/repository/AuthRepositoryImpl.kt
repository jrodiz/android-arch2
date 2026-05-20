package com.rodiz.arch2.feature.login.data.repository

import android.net.Uri
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.firebase.UserProfileRepository
import com.rodiz.arch2.core.firebase.model.UserProfile
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.login.data.local.CredentialVault
import com.rodiz.arch2.feature.login.data.mapper.toSession
import com.rodiz.arch2.feature.login.data.remote.AvatarUploader
import com.rodiz.arch2.feature.login.data.remote.FakeAuthRemoteDataSource
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.model.SignUpRequest
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
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
    private val firebaseAuth: FirebaseAuth,
    private val userProfileRepository: UserProfileRepository,
    private val avatarUploader: AvatarUploader,
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

    override suspend fun signInWithGoogle(idToken: String): Try<Session, AuthError> = withContext(io) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return@withContext Try.Failure(AuthError.GoogleSignInFailed)
            val firebaseToken = user.getIdToken(false).await().token.orEmpty()
            val session = Session(
                userId = user.uid,
                token = firebaseToken,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString(),
            )
            sessionRepository.save(session)
            userProfileRepository.upsertOnSignIn(
                UserProfile(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString(),
                    provider = "google",
                ),
            )
            Try.Success(session)
        } catch (e: FirebaseNetworkException) {
            Try.Failure(AuthError.NoNetwork)
        } catch (e: FirebaseAuthException) {
            Try.Failure(AuthError.GoogleSignInFailed)
        } catch (e: IOException) {
            Try.Failure(AuthError.NoNetwork)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            Try.Failure(AuthError.Unknown)
        }
    }

    override suspend fun register(request: SignUpRequest): Try<Session, AuthError> = withContext(io) {
        try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(request.email.trim(), request.password)
                .await()
            val user = authResult.user ?: return@withContext Try.Failure(AuthError.Unknown)

            val avatarUrl: String? = request.avatarUri?.let { raw ->
                avatarUploader.upload(user.uid, Uri.parse(raw)).getOrNull()
            }
            val avatarUploadFailed = request.avatarUri != null && avatarUrl == null

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(request.displayName)
                    .setPhotoUri(avatarUrl?.let(Uri::parse))
                    .build(),
            ).await()

            val session = Session(
                userId = user.uid,
                token = user.getIdToken(false).await().token.orEmpty(),
                displayName = request.displayName,
                photoUrl = avatarUrl,
            )
            sessionRepository.save(session)
            userProfileRepository.upsertOnSignIn(
                UserProfile(
                    uid = user.uid,
                    email = request.email.trim(),
                    displayName = request.displayName,
                    photoUrl = avatarUrl,
                    provider = "password",
                ),
            )

            if (avatarUploadFailed) {
                Try.Failure(AuthError.AvatarUploadFailed)
            } else {
                Try.Success(session)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            Try.Failure(AuthError.EmailAlreadyInUse)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Try.Failure(AuthError.WeakPassword)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Try.Failure(AuthError.InvalidCredentials)
        } catch (e: FirebaseNetworkException) {
            Try.Failure(AuthError.NoNetwork)
        } catch (e: IOException) {
            Try.Failure(AuthError.NoNetwork)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            Try.Failure(AuthError.Unknown)
        }
    }

    override suspend fun hasStoredCredentials(): Boolean = withContext(io) { vault.exists() }

    private suspend fun performLogin(
        credentials: Credentials,
        persistForBiometric: Boolean,
    ): Try<Session, AuthError> = try {
        val result = firebaseAuth
            .signInWithEmailAndPassword(credentials.email.trim(), credentials.password)
            .await()
        val user = result.user ?: return Try.Failure(AuthError.Unknown)
        val token = user.getIdToken(false).await().token.orEmpty()
        val session = Session(
            userId = user.uid,
            token = token,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
        )
        sessionRepository.save(session)
        if (persistForBiometric) vault.store(credentials)
        Try.Success(session)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Try.Failure(AuthError.InvalidCredentials)
    } catch (e: FirebaseAuthException) {
        Try.Failure(AuthError.InvalidCredentials)
    } catch (e: FirebaseNetworkException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (e: SocketTimeoutException) {
        Try.Failure(AuthError.Timeout)
    } catch (e: TimeoutCancellationException) {
        Try.Failure(AuthError.Timeout)
    } catch (e: IOException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.Unknown)
    }
}

/** Lightweight HTTP exception used by fake/real auth sources to signal an HTTP error code. */
internal class HttpException(val code: Int) : RuntimeException("HTTP $code")
