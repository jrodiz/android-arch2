package com.rodiz.arch2.feature.deck.data.di

import com.rodiz.arch2.feature.deck.data.FirestoreDeckRepository
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DeckDataModule {
    @Binds
    @Singleton
    abstract fun bindDeckRepository(impl: FirestoreDeckRepository): DeckRepository
}
