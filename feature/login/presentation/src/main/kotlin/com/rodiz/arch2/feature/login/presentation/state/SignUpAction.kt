package com.rodiz.arch2.feature.login.presentation.state

sealed interface SignUpAction {
    data class FirstNameChanged(val value: String) : SignUpAction
    data class LastNameChanged(val value: String) : SignUpAction
    data class EmailChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
    data class ConfirmPasswordChanged(val value: String) : SignUpAction
    data object TogglePasswordVisibility : SignUpAction
    data object ToggleConfirmPasswordVisibility : SignUpAction
    data object PickAvatarTapped : SignUpAction
    data object PickFromGallery : SignUpAction
    data object PickFromCamera : SignUpAction
    data class AvatarSelected(val uri: String) : SignUpAction
    data object AvatarCleared : SignUpAction
    data object DismissAvatarSheet : SignUpAction
    data object Submit : SignUpAction
    data object BackTapped : SignUpAction
    data object DismissError : SignUpAction
}
