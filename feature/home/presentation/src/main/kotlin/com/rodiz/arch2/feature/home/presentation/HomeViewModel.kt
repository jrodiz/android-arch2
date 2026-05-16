package com.rodiz.arch2.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.home.domain.model.PostId
import com.rodiz.arch2.feature.home.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val postRepository: PostRepository,
) : ViewModel() {

    val session: StateFlow<Session?> = sessionRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val refreshState = MutableStateFlow(RefreshState())

    val uiState: StateFlow<HomeUiState> = combine(
        postRepository.observeFeed(),
        refreshState,
    ) { posts, refresh ->
        when {
            refresh.error != null && posts.isEmpty() -> HomeUiState.Error(refresh.error)
            posts.isEmpty() && refresh.firstLoadComplete -> HomeUiState.Empty
            posts.isEmpty() -> HomeUiState.Loading
            else -> HomeUiState.Content(
                posts = posts.map { it.toUiModel() },
                isRefreshing = refresh.inFlight,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _transientError = MutableStateFlow<String?>(null)
    val transientError: StateFlow<String?> = _transientError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.update { it.copy(inFlight = true, error = null) }
            runCatching { postRepository.refresh() }
                .onSuccess {
                    refreshState.update {
                        it.copy(inFlight = false, firstLoadComplete = true, error = null)
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Something went wrong"
                    refreshState.update {
                        it.copy(inFlight = false, firstLoadComplete = true, error = message)
                    }
                    if (uiState.value is HomeUiState.Content) {
                        _transientError.value = message
                    }
                }
        }
    }

    fun onLikeClicked(postId: PostId) {
        viewModelScope.launch {
            postRepository.toggleLike(postId)
        }
    }

    fun consumeTransientError() {
        _transientError.value = null
    }

    private data class RefreshState(
        val inFlight: Boolean = false,
        val firstLoadComplete: Boolean = false,
        val error: String? = null,
    )
}
