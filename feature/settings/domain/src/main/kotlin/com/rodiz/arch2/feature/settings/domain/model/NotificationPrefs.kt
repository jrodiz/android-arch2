package com.rodiz.arch2.feature.settings.domain.model

data class NotificationPrefs(
    val newMatch: Boolean,
    val newMessage: Boolean,
    val someoneLiked: Boolean,
    val weeklyDigest: Boolean,
) {
    companion object {
        val DEFAULT = NotificationPrefs(
            newMatch = true,
            newMessage = true,
            someoneLiked = true,
            weeklyDigest = false,
        )
    }
}
