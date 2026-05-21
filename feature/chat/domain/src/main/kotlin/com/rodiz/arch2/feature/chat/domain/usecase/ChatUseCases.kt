package com.rodiz.arch2.feature.chat.domain.usecase

import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.chat.domain.model.ReportReason
import com.rodiz.arch2.feature.chat.domain.repository.ChatRepository
import com.rodiz.arch2.feature.match.domain.model.MatchId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveChatUseCase @Inject constructor(
    private val repo: ChatRepository,
) {
    operator fun invoke(matchId: MatchId): Flow<List<Message>> = repo.observeChat(matchId)
}

class SendMessageUseCase @Inject constructor(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(matchId: MatchId, text: String): Message =
        repo.sendMessage(matchId, text)
}

class MarkAllReadUseCase @Inject constructor(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(matchId: MatchId) = repo.markAllRead(matchId)
}

class BlockOtherUseCase @Inject constructor(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(matchId: MatchId) = repo.blockOther(matchId)
}

class ReportOtherUseCase @Inject constructor(
    private val repo: ChatRepository,
) {
    suspend operator fun invoke(matchId: MatchId, reason: ReportReason, freeText: String?) =
        repo.reportOther(matchId, reason, freeText)
}
