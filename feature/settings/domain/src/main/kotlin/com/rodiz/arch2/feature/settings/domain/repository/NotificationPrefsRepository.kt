package com.rodiz.arch2.feature.settings.domain.repository

import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs
import kotlinx.coroutines.flow.Flow

interface NotificationPrefsRepository {
    /** Live stream of the signed-in owner's notification prefs. Emits DEFAULT when missing. */
    fun observePrefs(): Flow<NotificationPrefs>

    /** Persist all four prefs at once (cheap merge write under owners/{me}.notifications). */
    suspend fun updatePrefs(prefs: NotificationPrefs)
}
