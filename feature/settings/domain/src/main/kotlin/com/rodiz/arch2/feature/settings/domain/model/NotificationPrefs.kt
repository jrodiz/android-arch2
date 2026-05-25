package com.rodiz.arch2.feature.settings.domain.model

data class NotificationPrefs(
    val newMatch: Boolean,
    val newMessage: Boolean,
    val someoneLiked: Boolean,
    val weeklyDigest: Boolean,
    val quietHoursEnabled: Boolean,
    val quietHoursStartMinutes: Int,
    val quietHoursEndMinutes: Int,
    /**
     * IANA timezone id (e.g. "America/New_York") used by the Cloud Function
     * to evaluate the quiet-hours window against the user's wall clock. Null
     * for prefs written before this field existed — the function falls back
     * to UTC in that case (the prior behavior).
     */
    val timezone: String? = null,
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
            timezone = null,
        )
    }
}
