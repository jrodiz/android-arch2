package com.rodiz.arch2.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.chat.domain.model.ReportReason
import com.rodiz.arch2.feature.chat.domain.usecase.BlockOtherUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.MarkAllReadUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.ObserveChatUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.ReportOtherUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.SendMessageUseCase
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.usecase.ObserveMatchUseCase
import com.rodiz.arch2.feature.match.domain.usecase.UnmatchUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val unmatched: Boolean = false,
    val errorMessage: String? = null,
    val currentUid: String = "",
    val isReporting: Boolean = false,
    val reportSubmittedAtMillis: Long? = null,
)

internal class ChatViewModel @AssistedInject constructor(
    @Assisted matchIdValue: String,
    observeChat: ObserveChatUseCase,
    observeMatch: ObserveMatchUseCase,
    private val sendMessage: SendMessageUseCase,
    private val markAllRead: MarkAllReadUseCase,
    private val unmatch: UnmatchUseCase,
    private val blockOther: BlockOtherUseCase,
    private val reportOther: ReportOtherUseCase,
    private val sessionRepo: SessionRepository,
) : ViewModel() {

    private val matchId: MatchId = MatchId(matchIdValue)

    @AssistedFactory
    interface Factory {
        fun create(matchIdValue: String): ChatViewModel
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _exited = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exited: SharedFlow<Unit> = _exited.asSharedFlow()

    init {
        viewModelScope.launch {
            val uid = sessionRepo.current()?.userId.orEmpty()
            _uiState.update { it.copy(currentUid = uid) }
        }
        viewModelScope.launch {
            observeChat(matchId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { msgs ->
                    _uiState.update { it.copy(messages = msgs) }
                    runCatching { markAllRead(matchId) }
                }
        }
        viewModelScope.launch {
            observeMatch(matchId)
                .catch { /* ignore */ }
                .collect { match ->
                    if (match == null) _uiState.update { it.copy(unmatched = true) }
                }
        }
    }

    fun draftChanged(value: String) {
        _uiState.update { it.copy(draft = value.take(2000)) }
    }

    fun send() {
        val draft = _uiState.value.draft.trim()
        if (draft.isEmpty() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            runCatching { sendMessage(matchId, draft) }
                .onSuccess { _uiState.update { it.copy(isSending = false, draft = "") } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false, errorMessage = e.message ?: "Send failed") }
                }
        }
    }

    fun unmatchAndExit() {
        viewModelScope.launch {
            runCatching { unmatch(matchId) }
                .onSuccess { _exited.tryEmit(Unit) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun blockAndExit() {
        viewModelScope.launch {
            runCatching { blockOther(matchId) }
                .onSuccess { _exited.tryEmit(Unit) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Block failed") } }
        }
    }

    fun submitReport(reason: ReportReason, freeText: String?) {
        if (_uiState.value.isReporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isReporting = true) }
            runCatching { reportOther(matchId, reason, freeText) }
                .onSuccess {
                    _uiState.update {
                        it.copy(isReporting = false, reportSubmittedAtMillis = System.currentTimeMillis())
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isReporting = false, errorMessage = e.message ?: "Report failed") }
                }
        }
    }

    fun clearReportSubmitted() {
        _uiState.update { it.copy(reportSubmittedAtMillis = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
