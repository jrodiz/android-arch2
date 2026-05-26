package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.MAX_FEATURED_PETS
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class FeaturedPetsUiState(
    val pets: List<PetRow> = emptyList(),
    val featuredCount: Int = 0,
    val atLimit: Boolean = false,
    val isLoading: Boolean = true,
    /** Sentinel for the "you can feature up to 3 pets" snackbar. */
    val snackbarMessageRes: Int? = null,
)

internal data class PetRow(
    val pet: Pet,
    val isFeatured: Boolean,
)

@HiltViewModel
internal class FeaturedPetsViewModel @Inject constructor(
    private val observeMyPets: ObserveMyPetsUseCase,
    private val featuredRepo: FeaturedPetsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeaturedPetsUiState())
    val uiState: StateFlow<FeaturedPetsUiState> = _uiState.asStateFlow()

    private var refreshed = false

    init {
        viewModelScope.launch {
            combine(
                observeMyPets(setOf(PetState.ACTIVE)).catch { emit(emptyList()) },
                featuredRepo.observe().catch { /* keep last UiState — snackbar still works */ },
            ) { pets, state ->
                val featuredIds = state.featured.mapTo(mutableSetOf()) { it.id }
                FeaturedPetsUiState(
                    pets = pets.map { PetRow(pet = it, isFeatured = it.id.value in featuredIds) },
                    featuredCount = featuredIds.size,
                    atLimit = featuredIds.size >= MAX_FEATURED_PETS,
                    isLoading = false,
                )
            }.collect { fresh ->
                _uiState.update { previous ->
                    fresh.copy(snackbarMessageRes = previous.snackbarMessageRes)
                }
                // Same once-per-VM refresh pattern as MyPetsViewModel — reconcile
                // cache against the authoritative list on the first emission.
                if (!refreshed) {
                    refreshed = true
                    featuredRepo.refreshFrom(
                        fresh.pets.associate { row ->
                            row.pet.id.value to row.pet.toFeaturedPet()
                        },
                    )
                }
            }
        }
    }

    fun onToggle(pet: Pet) {
        viewModelScope.launch {
            val currentlyFeatured = _uiState.value.pets
                .firstOrNull { it.pet.id == pet.id }?.isFeatured == true
            if (currentlyFeatured) {
                featuredRepo.unpin(pet.id.value)
            } else {
                val accepted = featuredRepo.pin(pet.toFeaturedPet())
                if (!accepted) {
                    _uiState.update { it.copy(snackbarMessageRes = R.string.settings_featured_limit_reached) }
                }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessageRes = null) }
    }
}

private fun Pet.toFeaturedPet(): FeaturedPet = FeaturedPet(
    id = id.value,
    name = name,
    species = species.name,
    avatarUrl = photos.firstOrNull()?.let { (it.source as? PhotoSource.Remote)?.downloadUrl },
)
