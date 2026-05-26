package com.rodiz.arch2.feature.notifications.nav

import kotlinx.serialization.Serializable

/** Reached via Settings → Notifications. onDone returns to the previous screen. */
@Serializable
data object NotificationRationale

/**
 * Combined permissions screen reached as the last step of the post-signup flow.
 * Asks for location + notifications in a single surface. onDone lands on DeckHome
 * regardless of what the user granted (both optional, see plans/onboarding-permissions-screen.md).
 */
@Serializable
data object PermissionsOnboarding
