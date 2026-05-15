package com.rodiz.arch2.di

import com.rodiz.arch2.core.common.coroutine.DefaultDispatcher
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.coroutine.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides @Singleton @IoDispatcher
    fun provideIo(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @DefaultDispatcher
    fun provideDefault(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton @MainDispatcher
    fun provideMain(): CoroutineDispatcher = Dispatchers.Main
}
