package com.rodiz.arch2.feature.login.data.mapper

import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.data.remote.dto.AuthDto

internal fun AuthDto.toSession(): Session = Session(userId = userId, token = token)
