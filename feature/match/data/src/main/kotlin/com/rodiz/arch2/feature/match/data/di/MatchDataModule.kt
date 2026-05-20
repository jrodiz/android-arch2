package com.rodiz.arch2.feature.match.data.di

import com.rodiz.arch2.feature.match.data.FirestoreMatchRepository
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MatchDataModule {
    @Binds
    @Singleton
    abstract fun bindMatchRepository(impl: FirestoreMatchRepository): MatchRepository
}
