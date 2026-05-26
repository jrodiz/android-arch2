package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsState
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.SetPausedUseCase
import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs
import com.rodiz.arch2.feature.settings.domain.usecase.ObserveBlockedOwnersUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.ObserveNotificationPrefsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SettingsHomeUiState(
    val isLoading: Boolean = true,
    val notifEnabledCount: Int = 0,
    val blockedCount: Int = 0,
    val featuredCount: Int = 0,
    val paused: Boolean = false,
    val isPausing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class SettingsHomeViewModel @Inject constructor(
    observeNotificationPrefs: ObserveNotificationPrefsUseCase,
    observeBlockedOwners: ObserveBlockedOwnersUseCase,
    observeMyProfile: ObserveMyProfileUseCase,
    private val featuredPetsRepository: FeaturedPetsRepository,
    private val setPaused: SetPausedUseCase,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsHomeUiState())
    val uiState: StateFlow<SettingsHomeUiState> = _uiState.asStateFlow()

    init {
        // Each upstream gets its own `catch` so a single Firestore hiccup (e.g.
        // a transient permission denial during sign-out) doesn't blank the entire
        // home — the rest of the screen keeps rendering with last-known values.
        val notifFlow = observeNotificationPrefs()
            .catch { emit(NotificationPrefs.DEFAULT) }
        val blockedFlow = observeBlockedOwners()
            .catch { emit(emptyList()) }
        val profileFlow = observeMyProfile()
            .catch { emit(null) }

        viewModelScope.launch {
            combine(notifFlow, blockedFlow, profileFlow) { prefs, blocked, profile ->
                val notifCount = listOf(
                    prefs.newMatch,
                    prefs.newMessage,
                    prefs.someoneLiked,
                    prefs.weeklyDigest,
                ).count { it }
                Triple(notifCount, blocked.size, profile?.paused ?: false)
            }.collect { (notifCount, blockedCount, paused) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notifEnabledCount = notifCount,
                        blockedCount = blockedCount,
                        // Don't clobber an optimistic local pause toggle that's
                        // still in flight; the upstream profile flow will catch
                        // up after the write commits.
                        paused = if (it.isPausing) it.paused else paused,
                    )
                }
            }
        }
        // Featured pets count drives the trailing CountPill on the new
        // "Featured on login" row — kept on its own collector so a Firestore
        // hiccup on one of the other flows doesn't blank the pin count.
        viewModelScope.launch {
            featuredPetsRepository.observe()
                .catch { emit(FeaturedPetsState()) }
                .collect { state ->
                    _uiState.update { it.copy(featuredCount = state.featured.size) }
                }
        }
    }

    fun onTogglePause(value: Boolean) {
        if (_uiState.value.isPausing) return
        _uiState.update { it.copy(paused = value, isPausing = true) }
        viewModelScope.launch {
            runCatching { setPaused(value) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Could not save")
                    }
                }
            _uiState.update { it.copy(isPausing = false) }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.clear()
            onSignedOut()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
