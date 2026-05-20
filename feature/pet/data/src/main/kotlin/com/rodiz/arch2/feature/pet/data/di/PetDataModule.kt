package com.rodiz.arch2.feature.pet.data.di

import com.rodiz.arch2.feature.pet.data.FirestorePetRepository
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PetDataModule {
    @Binds
    @Singleton
    abstract fun bindPetRepository(impl: FirestorePetRepository): PetRepository
}
