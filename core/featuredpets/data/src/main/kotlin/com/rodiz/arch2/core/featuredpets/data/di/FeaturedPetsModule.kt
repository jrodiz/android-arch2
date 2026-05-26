package com.rodiz.arch2.core.featuredpets.data.di

import com.rodiz.arch2.core.featuredpets.data.DataStoreFeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeaturedPetsModule {
    @Binds
    @Singleton
    abstract fun bindFeaturedPetsRepository(
        impl: DataStoreFeaturedPetsRepository,
    ): FeaturedPetsRepository
}
