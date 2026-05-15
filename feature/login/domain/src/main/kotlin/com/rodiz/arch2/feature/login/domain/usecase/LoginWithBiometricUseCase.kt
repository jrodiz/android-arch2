package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

class LoginWithBiometricUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Try<Session, AuthError> =
        repository.loginWithStoredCredentials()
}
