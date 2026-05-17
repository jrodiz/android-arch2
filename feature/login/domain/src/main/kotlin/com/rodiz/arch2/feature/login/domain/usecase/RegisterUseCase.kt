package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.SignUpRequest
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: SignUpRequest): Try<Session, AuthError> =
        repository.register(request)
}
