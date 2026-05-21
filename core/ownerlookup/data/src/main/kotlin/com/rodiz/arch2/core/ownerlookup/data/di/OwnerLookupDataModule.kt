package com.rodiz.arch2.core.ownerlookup.data.di

import com.rodiz.arch2.core.ownerlookup.data.FirestoreOwnerLookupRepository
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OwnerLookupDataModule {
    @Binds
    @Singleton
    abstract fun bindOwnerLookupRepository(impl: FirestoreOwnerLookupRepository): OwnerLookupRepository
}
