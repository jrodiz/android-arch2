package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateAvatarUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateFirstNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EditProfileUiState(
    val isLoading: Boolean = true,
    val firstNameField: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val errorMessage: String? = null,
    val savedAtMillis: Long? = null,
    val original: OwnerProfile? = null,
) {
    val isNameValid: Boolean = firstNameField.trim().length in 1..30
    val isDirty: Boolean = original?.let { it.firstName != firstNameField.trim() } ?: false
    val canSave: Boolean = isNameValid && isDirty && !isSaving
}

@HiltViewModel
internal class EditProfileViewModel @Inject constructor(
    observeMyProfile: ObserveMyProfileUseCase,
    private val updateFirstName: UpdateFirstNameUseCase,
    private val updateAvatar: UpdateAvatarUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            observeMyProfile()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { profile ->
                    _uiState.update { current ->
                        // Only seed the editable fields on the first emission. Subsequent
                        // emissions (e.g. after a successful avatar upload) refresh the
                        // baseline so dirty calculation stays correct, but keep whatever
                        // the user has currently typed.
                        if (current.original == null) {
                            current.copy(
                                isLoading = false,
                                firstNameField = profile?.firstName.orEmpty(),
                                avatarUrl = profile?.avatarUrl,
                                original = profile,
                            )
                        } else {
                            current.copy(
                                isLoading = false,
                                avatarUrl = profile?.avatarUrl ?: current.avatarUrl,
                                original = profile,
                            )
                        }
                    }
                }
        }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstNameField = value.take(30)) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching { updateFirstName(state.firstNameField) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, savedAtMillis = System.currentTimeMillis()) }
                    _saved.tryEmit(Unit)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Save failed") }
                }
        }
    }

    fun onAvatarPicked(localUri: String) {
        if (_uiState.value.isUploadingAvatar) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true) }
            runCatching { updateAvatar(localUri) }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Avatar upload failed") }
                }
            _uiState.update { it.copy(isUploadingAvatar = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
