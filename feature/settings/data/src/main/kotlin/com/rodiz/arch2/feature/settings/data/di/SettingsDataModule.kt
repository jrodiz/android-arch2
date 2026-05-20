package com.rodiz.arch2.feature.settings.data.di

import com.rodiz.arch2.feature.settings.data.FirestoreAccountDeletionRepository
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsDataModule {
    @Binds
    @Singleton
    abstract fun bindAccountDeletionRepository(impl: FirestoreAccountDeletionRepository): AccountDeletionRepository
}
