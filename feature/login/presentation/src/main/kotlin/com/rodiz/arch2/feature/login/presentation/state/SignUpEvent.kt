package com.rodiz.arch2.feature.login.presentation.state

sealed interface SignUpEvent {
    data object NavigateHome : SignUpEvent
    data object NavigateBack : SignUpEvent
    data object LaunchGalleryPicker : SignUpEvent
    data object LaunchCamera : SignUpEvent
    data class ShowSoftWarning(val warning: SoftWarning) : SignUpEvent
}

enum class SoftWarning { AvatarUploadFailed }
