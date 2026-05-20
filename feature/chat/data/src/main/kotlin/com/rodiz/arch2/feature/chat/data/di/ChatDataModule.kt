package com.rodiz.arch2.feature.chat.data.di

import com.rodiz.arch2.feature.chat.data.FirestoreChatRepository
import com.rodiz.arch2.feature.chat.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ChatDataModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: FirestoreChatRepository): ChatRepository
}
