package com.rodiz.arch2.feature.settings.domain.model

data class NotificationPrefs(
    val newMatch: Boolean,
    val newMessage: Boolean,
    val someoneLiked: Boolean,
    val weeklyDigest: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStartMinutes: Int,
    val quietHoursEndMinutes: Int,
) {
    companion object {
        val DEFAULT = NotificationPrefs(
            newMatch = true,
            newMessage = true,
            someoneLiked = true,
            weeklyDigest = false,
            quietHoursEnabled = false,
            quietHoursStartMinutes = 22 * 60,
            quietHoursEndMinutes = 8 * 60,
        )
    }
}
