package com.rodiz.arch2.core.petlookup.data.di

import com.rodiz.arch2.core.petlookup.data.FirestorePetLookupRepository
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PetLookupDataModule {
    @Binds
    @Singleton
    abstract fun bindPetLookupRepository(impl: FirestorePetLookupRepository): PetLookupRepository
}
