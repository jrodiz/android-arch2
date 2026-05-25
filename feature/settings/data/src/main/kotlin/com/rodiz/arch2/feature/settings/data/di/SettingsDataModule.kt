package com.rodiz.arch2.feature.settings.data.di

import com.rodiz.arch2.feature.settings.data.FirestoreAccountDeletionRepository
import com.rodiz.arch2.feature.settings.data.FirestoreBlockRepository
import com.rodiz.arch2.feature.settings.data.FirestoreDataExportRepository
import com.rodiz.arch2.feature.settings.data.FirestoreNotificationPrefsRepository
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import com.rodiz.arch2.feature.settings.domain.repository.BlockRepository
import com.rodiz.arch2.feature.settings.domain.repository.DataExportRepository
import com.rodiz.arch2.feature.settings.domain.repository.NotificationPrefsRepository
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

    @Binds
    @Singleton
    abstract fun bindNotificationPrefsRepository(impl: FirestoreNotificationPrefsRepository): NotificationPrefsRepository

    @Binds
    @Singleton
    abstract fun bindBlockRepository(impl: FirestoreBlockRepository): BlockRepository

    @Binds
    @Singleton
    abstract fun bindDataExportRepository(impl: FirestoreDataExportRepository): DataExportRepository
}
