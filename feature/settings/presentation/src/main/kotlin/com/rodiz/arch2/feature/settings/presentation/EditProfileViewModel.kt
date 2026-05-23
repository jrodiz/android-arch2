package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.common.geo.Geohash
import com.rodiz.arch2.feature.profile.domain.model.GeoPoint
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateAvatarUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateBioUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateFirstNameUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.UpdateLocationUseCase
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
    val bioField: String = "",
    val avatarUrl: String? = null,
    val email: String? = null,
    val location: GeoPoint? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isUpdatingLocation: Boolean = false,
    val errorMessage: String? = null,
    val savedAtMillis: Long? = null,
    val original: OwnerProfile? = null,
) {
    val isNameValid: Boolean = firstNameField.trim().length in 1..30
    val isBioValid: Boolean = bioField.length <= MAX_BIO_LENGTH
    val isDirty: Boolean = original?.let { orig ->
        orig.firstName != firstNameField.trim() || orig.bio != bioField.trim()
    } ?: false
    val canSave: Boolean = isNameValid && isBioValid && isDirty && !isSaving
    /**
     * Email-verified state is a UI-only signal for the redesign. Real Firebase
     * `currentUser.isEmailVerified` plumbing lands in a follow-up — for now we treat
     * "has an email on file" as verified so the populated state renders correctly.
     */
    val isEmailVerified: Boolean = !email.isNullOrBlank()

    companion object {
        const val MAX_BIO_LENGTH: Int = 150
        const val MAX_FIRST_NAME_LENGTH: Int = 30
    }
}

@HiltViewModel
internal class EditProfileViewModel @Inject constructor(
    observeMyProfile: ObserveMyProfileUseCase,
    private val updateFirstName: UpdateFirstNameUseCase,
    private val updateBio: UpdateBioUseCase,
    private val updateAvatar: UpdateAvatarUseCase,
    private val updateLocationUseCase: UpdateLocationUseCase,
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
                        // emissions (e.g. after a successful avatar/location upload) refresh
                        // the baseline so dirty calculation stays correct, but keep whatever
                        // the user has currently typed in the editable fields.
                        if (current.original == null) {
                            current.copy(
                                isLoading = false,
                                firstNameField = profile?.firstName.orEmpty(),
                                bioField = profile?.bio.orEmpty(),
                                avatarUrl = profile?.avatarUrl,
                                email = profile?.email,
                                location = profile?.location,
                                original = profile,
                            )
                        } else {
                            current.copy(
                                isLoading = false,
                                avatarUrl = profile?.avatarUrl ?: current.avatarUrl,
                                email = profile?.email ?: current.email,
                                location = profile?.location ?: current.location,
                                original = profile,
                            )
                        }
                    }
                }
        }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstNameField = value.take(EditProfileUiState.MAX_FIRST_NAME_LENGTH)) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bioField = value.take(EditProfileUiState.MAX_BIO_LENGTH)) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val originalName = state.original?.firstName.orEmpty()
        val originalBio = state.original?.bio.orEmpty()
        val newName = state.firstNameField.trim()
        val newBio = state.bioField.trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            runCatching {
                if (newName != originalName) updateFirstName(newName)
                if (newBio != originalBio) updateBio(newBio)
            }
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

    fun onLocationFetched(lat: Double, lng: Double, cityLabel: String?) {
        if (_uiState.value.isUpdatingLocation) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocation = true) }
            runCatching {
                val point = GeoPoint(
                    lat = lat,
                    lng = lng,
                    geohash = Geohash.encode(lat, lng, precision = 6),
                    cityLabel = cityLabel,
                )
                updateLocationUseCase(point)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save location") }
            }
            _uiState.update { it.copy(isUpdatingLocation = false) }
        }
    }

    fun onLocationError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
