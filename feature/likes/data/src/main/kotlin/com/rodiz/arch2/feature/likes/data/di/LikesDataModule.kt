package com.rodiz.arch2.feature.likes.data.di

import com.rodiz.arch2.feature.likes.data.FirestoreLikesYouRepository
import com.rodiz.arch2.feature.likes.domain.repository.LikesYouRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LikesDataModule {
    @Binds
    @Singleton
    abstract fun bindLikesYouRepository(impl: FirestoreLikesYouRepository): LikesYouRepository
}
