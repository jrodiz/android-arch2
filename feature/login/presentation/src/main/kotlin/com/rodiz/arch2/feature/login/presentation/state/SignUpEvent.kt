package com.rodiz.arch2.feature.login.presentation.state

sealed interface SignUpEvent {
    data object NavigateHome : SignUpEvent
    data object NavigateBack : SignUpEvent
    data object LaunchGalleryPicker : SignUpEvent
    data object LaunchCamera : SignUpEvent
    data class ShowSoftWarning(val warning: SoftWarning) : SignUpEvent
    /** Transient snackbar hint, identified by a string resource id resolved at render time. */
    data class ShowComingSoon(val resId: Int) : SignUpEvent
    /** Caller navigates to the Terms of Service page. */
    data object OpenTerms : SignUpEvent
    /** Caller navigates to the Privacy Policy page. */
    data object OpenPrivacyPolicy : SignUpEvent
}

enum class SoftWarning { AvatarUploadFailed }
