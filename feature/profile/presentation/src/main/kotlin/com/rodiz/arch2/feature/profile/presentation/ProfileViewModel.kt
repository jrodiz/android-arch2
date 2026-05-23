package com.rodiz.arch2.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.usecase.CancelAccountDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.ObservePendingDeletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: OwnerProfile? = null,
    val isLoading: Boolean = true,
    val pets: List<Pet> = emptyList(),
    val isPetsLoading: Boolean = true,
    val pendingDeletion: AccountDeletion? = null,
    val isCancellingDeletion: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    observeMyProfile: ObserveMyProfileUseCase,
    observeMyPets: ObserveMyPetsUseCase,
    observePendingDeletion: ObservePendingDeletionUseCase,
    private val cancelDeletion: CancelAccountDeletionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMyProfile()
                .catch { /* header collapses to placeholder; not fatal */ }
                .collect { profile ->
                    _uiState.update { it.copy(profile = profile, isLoading = false) }
                }
        }
        viewModelScope.launch {
            // Active pets only — the rail and the "X pets" subtitle ignore ARCHIVED.
            observeMyPets(filter = setOf(PetState.ACTIVE))
                .catch {
                    // Pet rail collapses gracefully on error — Profile is still useful
                    // even if we can't show the user's pets.
                    _uiState.update { it.copy(pets = emptyList(), isPetsLoading = false) }
                }
                .collect { pets ->
                    _uiState.update { it.copy(pets = pets, isPetsLoading = false) }
                }
        }
        viewModelScope.launch {
            observePendingDeletion()
                .catch { /* swallow — banner just stays hidden */ }
                .collect { deletion ->
                    _uiState.update { it.copy(pendingDeletion = deletion) }
                }
        }
    }

    fun cancelPendingDeletion() {
        if (_uiState.value.isCancellingDeletion) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingDeletion = true) }
            runCatching { cancelDeletion() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Could not cancel") }
                }
            _uiState.update { it.copy(isCancellingDeletion = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.clear()
            onSignedOut()
        }
    }
}
