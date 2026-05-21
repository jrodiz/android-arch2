package com.rodiz.arch2.core.filters.domain

import kotlinx.coroutines.flow.Flow

interface FilterPrefsRepository {
    /** Live filter prefs. Emits DEFAULT until the store is hydrated. */
    fun observePrefs(): Flow<FilterPrefs>

    /** Persists the full prefs object — write costs are tiny so we don't bother diffing. */
    suspend fun updatePrefs(prefs: FilterPrefs)
}
