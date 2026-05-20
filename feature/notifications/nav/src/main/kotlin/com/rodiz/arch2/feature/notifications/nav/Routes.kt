package com.rodiz.arch2.feature.notifications.nav

import kotlinx.serialization.Serializable

/** Reached via Settings → Notifications. onDone returns to the previous screen. */
@Serializable
data object NotificationRationale

/** Reached as the last step of the post-signup flow. onDone lands on DeckHome. */
@Serializable
data object NotificationRationaleOnboarding
