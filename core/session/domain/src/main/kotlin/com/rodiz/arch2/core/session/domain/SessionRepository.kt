package com.rodiz.arch2.core.session.domain

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observe(): Flow<Session?>
    suspend fun current(): Session?
    suspend fun save(session: Session)
    suspend fun clear()
}
