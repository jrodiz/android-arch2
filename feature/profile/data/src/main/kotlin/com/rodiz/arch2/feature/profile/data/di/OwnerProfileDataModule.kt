package com.rodiz.arch2.feature.profile.data.di

import com.rodiz.arch2.feature.profile.data.FirestoreOwnerProfileRepository
import com.rodiz.arch2.feature.profile.domain.repository.OwnerProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OwnerProfileDataModule {
    @Binds
    @Singleton
    abstract fun bindOwnerProfileRepository(
        impl: FirestoreOwnerProfileRepository,
    ): OwnerProfileRepository
}
