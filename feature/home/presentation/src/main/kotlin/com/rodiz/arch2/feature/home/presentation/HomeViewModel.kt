package com.rodiz.arch2.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val session: StateFlow<Session?> = sessionRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.clear()
            onSignedOut()
        }
    }
}
