package com.rodiz.arch2.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.RestorePetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class MyPetsUiState(
    val activePets: List<Pet> = emptyList(),
    val archivedPets: List<Pet> = emptyList(),
    /** Ids of pets currently featured on the Login screen; drives the pin overlay tint. */
    val featuredIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /**
     * Sentinel for the "you can feature up to 3 pets" snackbar — UI consumes it
     * via a `LaunchedEffect(snackbarMessage)` and clears via [onSnackbarShown].
     * Held separately from [errorMessage] because it's an in-band hint, not an
     * error condition.
     */
    val snackbarMessageRes: Int? = null,
)

@HiltViewModel
internal class MyPetsViewModel @Inject constructor(
    private val observeMyPets: ObserveMyPetsUseCase,
    private val restorePet: RestorePetUseCase,
    private val featuredRepo: FeaturedPetsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPetsUiState())
    val uiState: StateFlow<MyPetsUiState> = _uiState.asStateFlow()

    private var refreshedFeaturedFromSnapshot = false

    init {
        viewModelScope.launch {
            observeMyPets(filter = setOf(PetState.ACTIVE, PetState.ARCHIVED))
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { pets ->
                    val activePets = pets.filter { p -> p.state == PetState.ACTIVE }
                    _uiState.update {
                        it.copy(
                            activePets = activePets,
                            archivedPets = pets.filter { p -> p.state == PetState.ARCHIVED },
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                    // Post-auth: reconcile cached featured metadata against the
                    // authoritative list. Only the active set is eligible to be
                    // featured (archived/disabled pets don't show on Login).
                    if (!refreshedFeaturedFromSnapshot) {
                        refreshedFeaturedFromSnapshot = true
                        featuredRepo.refreshFrom(
                            activePets.associate { p -> p.id.value to p.toFeaturedPet() },
                        )
                    }
                }
        }
        // Mirror featured ids into UiState so PetThumbnailCard knows which pin
        // to render as active. Hot-shared with the Login VM's collector — both
        // observe the same DataStore-backed Flow.
        viewModelScope.launch {
            featuredRepo.observe()
                .catch { /* swallow — pin overlays just stay inactive */ }
                .collect { state ->
                    _uiState.update {
                        it.copy(featuredIds = state.featured.mapTo(mutableSetOf()) { fp -> fp.id })
                    }
                }
        }
    }

    fun restore(pet: Pet) {
        viewModelScope.launch {
            runCatching { restorePet(pet.id) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    /**
     * Toggle a pet's "featured on login" state. Returns no result — the UI
     * reacts via [uiState.featuredIds] and, on pin-limit, via [snackbarMessageRes].
     */
    fun onTogglePin(pet: Pet) {
        viewModelScope.launch {
            val currentlyPinned = _uiState.value.featuredIds.contains(pet.id.value)
            if (currentlyPinned) {
                featuredRepo.unpin(pet.id.value)
            } else {
                val accepted = featuredRepo.pin(pet.toFeaturedPet())
                if (!accepted) {
                    _uiState.update { it.copy(snackbarMessageRes = R.string.pet_featured_limit_reached) }
                }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessageRes = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

internal fun Pet.toFeaturedPet(): FeaturedPet = FeaturedPet(
    id = id.value,
    name = name,
    species = species.name,
    avatarUrl = photos.firstOrNull()?.let { (it.source as? PhotoSource.Remote)?.downloadUrl },
)
