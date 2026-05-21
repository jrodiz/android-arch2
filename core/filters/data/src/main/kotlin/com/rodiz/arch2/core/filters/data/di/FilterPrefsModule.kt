package com.rodiz.arch2.core.filters.data.di

import com.rodiz.arch2.core.filters.data.DataStoreFilterPrefsRepository
import com.rodiz.arch2.core.filters.domain.FilterPrefsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FilterPrefsModule {
    @Binds
    @Singleton
    abstract fun bindFilterPrefsRepository(impl: DataStoreFilterPrefsRepository): FilterPrefsRepository
}
