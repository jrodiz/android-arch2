package com.rodiz.arch2.feature.login.data.di

import com.rodiz.arch2.feature.login.data.repository.AuthRepositoryImpl
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
