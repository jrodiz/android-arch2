package com.rodiz.arch2.core.session.data

import com.rodiz.arch2.core.session.domain.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
