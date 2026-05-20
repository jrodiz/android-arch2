package com.rodiz.arch2.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetState
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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class MyPetsViewModel @Inject constructor(
    observeMyPets: ObserveMyPetsUseCase,
    private val restorePet: RestorePetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPetsUiState())
    val uiState: StateFlow<MyPetsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMyPets(filter = setOf(PetState.ACTIVE, PetState.ARCHIVED))
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { pets ->
                    _uiState.update {
                        it.copy(
                            activePets = pets.filter { p -> p.state == PetState.ACTIVE },
                            archivedPets = pets.filter { p -> p.state == PetState.ARCHIVED },
                            isLoading = false,
                            errorMessage = null,
                        )
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
