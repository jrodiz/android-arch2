package com.rodiz.arch2.feature.home.data.di

import com.rodiz.arch2.feature.home.data.FakePostRepository
import com.rodiz.arch2.feature.home.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostDataModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: FakePostRepository): PostRepository
}
