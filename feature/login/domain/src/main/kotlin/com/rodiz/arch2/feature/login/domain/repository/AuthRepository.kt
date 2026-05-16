package com.rodiz.arch2.feature.login.domain.repository

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials

interface AuthRepository {
    suspend fun login(credentials: Credentials): Try<Session, AuthError>
    suspend fun loginWithStoredCredentials(): Try<Session, AuthError>
    suspend fun signInWithGoogle(idToken: String): Try<Session, AuthError>
    suspend fun hasStoredCredentials(): Boolean
}
