package com.rodiz.arch2.feature.login.presentation.navigation

import com.rodiz.arch2.feature.login.domain.usecase.ValidateConfirmPasswordUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateNameUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ValidatorModule {
    @Provides @Singleton fun provideValidateEmail() = ValidateEmailUseCase()
    @Provides @Singleton fun provideValidatePassword() = ValidatePasswordUseCase()
    @Provides @Singleton fun provideValidateName() = ValidateNameUseCase()
    @Provides @Singleton fun provideValidateConfirmPassword() = ValidateConfirmPasswordUseCase()
}
