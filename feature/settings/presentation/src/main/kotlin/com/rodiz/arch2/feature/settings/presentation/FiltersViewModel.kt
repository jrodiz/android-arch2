package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.core.filters.domain.FilterPrefsRepository
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class FiltersUiState(
    val isLoading: Boolean = true,
    val prefs: FilterPrefs = FilterPrefs.DEFAULT,
)

@HiltViewModel
internal class FiltersViewModel @Inject constructor(
    private val repo: FilterPrefsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FiltersUiState())
    val uiState: StateFlow<FiltersUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observePrefs()
                .catch { /* swallow — UI stays on whatever it already had */ }
                .collect { prefs ->
                    if (debounceJob?.isActive != true) {
                        _uiState.update { it.copy(isLoading = false, prefs = prefs) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun setDistance(km: Int) = patch { it.copy(maxDistanceKm = km.coerceIn(5, 200)) }

    fun toggleIntent(intent: Intent) = patch {
        val next = if (intent in it.intents) it.intents - intent else it.intents + intent
        // Don't let the user empty all intents — the deck would always be empty.
        if (next.isEmpty()) it else it.copy(intents = next)
    }

    fun toggleSpecies(category: SpeciesCategory) = patch {
        val next = if (category in it.speciesCategories) it.speciesCategories - category
        else it.speciesCategories + category
        if (next.isEmpty()) it else it.copy(speciesCategories = next)
    }

    private fun patch(mutate: (FilterPrefs) -> FilterPrefs) {
        val next = mutate(_uiState.value.prefs)
        if (next == _uiState.value.prefs) return
        _uiState.update { it.copy(prefs = next) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(200)
            runCatching { repo.updatePrefs(next) }
        }
    }
}
