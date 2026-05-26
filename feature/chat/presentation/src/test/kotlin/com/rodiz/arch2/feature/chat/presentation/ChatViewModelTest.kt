package com.rodiz.arch2.feature.chat.presentation

import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.petlookup.domain.PetDisplay
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.chat.domain.model.MessageId
import com.rodiz.arch2.feature.chat.domain.model.ReportReason
import com.rodiz.arch2.feature.chat.domain.repository.ChatRepository
import com.rodiz.arch2.feature.chat.domain.usecase.BlockOtherUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.MarkAllReadUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.ObserveChatUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.ReportOtherUseCase
import com.rodiz.arch2.feature.chat.domain.usecase.SendMessageUseCase
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import com.rodiz.arch2.feature.match.domain.usecase.ObserveMatchUseCase
import com.rodiz.arch2.feature.match.domain.usecase.UnmatchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    private val matchIdValue = "m1"
    private val meUid = "me"
    private val themUid = "them"
    private val themPetId = "their-pet"

    @Test
    fun `init loads currentUid from session and surfaces incoming messages`() = runTest(testDispatcher) {
        val chat = FakeChatRepo()
        val vm = newViewModel(chatRepo = chat)
        advanceUntilIdle()
        assertEquals(meUid, vm.uiState.value.currentUid)

        chat.messages.value = listOf(message("hello"))
        advanceUntilIdle()
        assertEquals(listOf("hello"), vm.uiState.value.messages.map { it.text })
    }

    @Test
    fun `each incoming snapshot triggers markAllRead for the match`() = runTest(testDispatcher) {
        val chat = FakeChatRepo()
        val vm = newViewModel(chatRepo = chat)
        advanceUntilIdle()
        val readsAfterInit = chat.markedReadCount
        chat.messages.value = listOf(message("hi"))
        advanceUntilIdle()
        assertTrue(chat.markedReadCount > readsAfterInit, "markAllRead fires on each new snapshot")
    }

    @Test
    fun `draftChanged trims to a 2000-char cap`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.draftChanged("a".repeat(2500))
        assertEquals(2000, vm.uiState.value.draft.length)
    }

    @Test
    fun `send is a no-op for empty or whitespace drafts`() = runTest(testDispatcher) {
        val chat = FakeChatRepo()
        val vm = newViewModel(chatRepo = chat)
        vm.draftChanged("   ")
        vm.send()
        advanceUntilIdle()
        assertEquals(0, chat.sendCount, "blank input must not hit the repository")
        assertFalse(vm.uiState.value.isSending)
    }

    @Test
    fun `send happy path clears draft and flips isSending`() = runTest(testDispatcher) {
        val chat = FakeChatRepo()
        val vm = newViewModel(chatRepo = chat)
        vm.draftChanged("hey")
        vm.send()
        advanceUntilIdle()
        assertEquals(1, chat.sendCount)
        val state = vm.uiState.value
        assertEquals("", state.draft)
        assertFalse(state.isSending)
    }

    @Test
    fun `send failure surfaces errorMessage and unblocks isSending`() = runTest(testDispatcher) {
        val chat = FakeChatRepo(sendError = RuntimeException("offline"))
        val vm = newViewModel(chatRepo = chat)
        vm.draftChanged("hey")
        vm.send()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("offline", state.errorMessage)
        assertFalse(state.isSending)
    }

    @Test
    fun `observed match populates state and never marks the user as unmatched`() = runTest(testDispatcher) {
        val match = FakeMatchRepo(match = matchOf())
        val vm = newViewModel(matchRepo = match)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state.match)
        assertFalse(state.unmatched)
    }

    @Test
    fun `match going null flips unmatched true`() = runTest(testDispatcher) {
        val match = FakeMatchRepo(match = matchOf())
        val vm = newViewModel(matchRepo = match)
        advanceUntilIdle()
        match.matches.value = null
        advanceUntilIdle()
        assertTrue(vm.uiState.value.unmatched)
    }

    @Test
    fun `owner + pet lookups populate header display fields`() = runTest(testDispatcher) {
        val ownerLookup = FakeOwnerLookup(themUid to OwnerDisplay(id = themUid, firstName = "Sam", avatarUrl = null))
        val petLookup = FakePetLookup(
            themPetId to PetDisplay(
                id = themPetId,
                ownerId = themUid,
                name = "Rex",
                species = "Dog",
                avatarUrl = null,
            ),
        )
        val vm = newViewModel(ownerLookup = ownerLookup, petLookup = petLookup)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("Sam", state.other?.firstName)
        assertEquals("Rex", state.otherPet?.name)
    }

    @Test
    fun `unmatchAndExit emits on exited shared flow when use case succeeds`() = runTest(testDispatcher) {
        val match = FakeMatchRepo(match = matchOf())
        val vm = newViewModel(matchRepo = match)
        advanceUntilIdle()
        var exited = false
        // backgroundScope is auto-cancelled when runTest ends — needed because
        // `exited` is a SharedFlow that never completes on its own.
        backgroundScope.launch { vm.exited.collect { exited = true } }
        vm.unmatchAndExit()
        advanceUntilIdle()
        assertTrue(exited, "exited should fire when unmatch succeeds")
    }

    @Test
    fun `blockAndExit surfaces errorMessage on failure and does not emit exited`() = runTest(testDispatcher) {
        val chat = FakeChatRepo(blockError = RuntimeException("permission denied"))
        val vm = newViewModel(chatRepo = chat)
        var exited = false
        backgroundScope.launch { vm.exited.collect { exited = true } }
        vm.blockAndExit()
        advanceUntilIdle()
        assertEquals("permission denied", vm.uiState.value.errorMessage)
        assertFalse(exited, "exited must not fire when the block use case throws")
    }

    @Test
    fun `submitReport happy path stamps reportSubmittedAtMillis and clears isReporting`() = runTest(testDispatcher) {
        val chat = FakeChatRepo()
        val vm = newViewModel(chatRepo = chat)
        vm.submitReport(ReportReason.SPAM, freeText = "lots of links")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state.reportSubmittedAtMillis)
        assertFalse(state.isReporting)
        assertEquals(ReportReason.SPAM to "lots of links", chat.lastReport)
    }

    @Test
    fun `submitReport is debounced while the first call is suspended`() = runTest(testDispatcher) {
        // Wedge the first report in a suspendCancellableCoroutine the test controls,
        // so we can fire a second submitReport while isReporting is still true.
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val chat = SuspendingReportChatRepo(gate)
        val vm = newViewModel(chatRepo = chat)
        vm.submitReport(ReportReason.HARASSMENT, null)
        // Run-current lets the first launch hit reportOther() and suspend on the gate,
        // which is exactly when isReporting=true is observable to the second call.
        testDispatcher.scheduler.runCurrent()
        vm.submitReport(ReportReason.FAKE_PROFILE, null)
        advanceUntilIdle()
        // Second tap saw isReporting=true and returned without calling the repo.
        assertEquals(1, chat.reportCount)
        // Now let the first report finish so the test doesn't leak the deferred.
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `clearReportSubmitted + clearError reset their fields`() = runTest(testDispatcher) {
        val chat = FakeChatRepo(sendError = RuntimeException("boom"))
        val vm = newViewModel(chatRepo = chat)
        vm.draftChanged("x")
        vm.send()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.submitReport(ReportReason.OTHER, null)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.reportSubmittedAtMillis)

        vm.clearError()
        vm.clearReportSubmitted()
        val state = vm.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.reportSubmittedAtMillis)
    }

    // ---- helpers ----

    private fun newViewModel(
        chatRepo: ChatRepository = FakeChatRepo(),
        matchRepo: MatchRepository = FakeMatchRepo(match = matchOf()),
        ownerLookup: OwnerLookupRepository = FakeOwnerLookup(),
        petLookup: PetLookupRepository = FakePetLookup(),
        session: SessionRepository = FakeSession(Session(userId = meUid, token = "t")),
    ): ChatViewModel = ChatViewModel(
        matchIdValue = matchIdValue,
        observeChat = ObserveChatUseCase(chatRepo),
        observeMatch = ObserveMatchUseCase(matchRepo),
        sendMessage = SendMessageUseCase(chatRepo),
        markAllRead = MarkAllReadUseCase(chatRepo),
        unmatch = UnmatchUseCase(matchRepo),
        blockOther = BlockOtherUseCase(chatRepo),
        reportOther = ReportOtherUseCase(chatRepo),
        sessionRepo = session,
        ownerLookup = ownerLookup,
        petLookup = petLookup,
    )

    private fun matchOf(): Match = Match(
        id = MatchId(matchIdValue),
        ownerAId = meUid,
        ownerBId = themUid,
        createdAt = Instant.fromEpochSeconds(1_000),
        lastMessageAt = null,
        lastMessagePreview = null,
        lastMessageFromOwnerId = null,
        petAId = "my-pet",
        petBId = themPetId,
    )

    private fun message(text: String) = Message(
        id = MessageId("msg-$text"),
        matchId = MatchId(matchIdValue),
        fromOwnerId = themUid,
        text = text,
        createdAt = Instant.fromEpochSeconds(2_000),
        readBy = emptyMap(),
    )

    private class FakeChatRepo(
        val sendError: Throwable? = null,
        val blockError: Throwable? = null,
        val reportError: Throwable? = null,
    ) : ChatRepository {
        val messages = MutableStateFlow<List<Message>>(emptyList())
        var sendCount = 0
        var markedReadCount = 0
        var reportCount = 0
        var lastReport: Pair<ReportReason, String?>? = null

        override fun observeChat(matchId: MatchId, pageSize: Int): Flow<List<Message>> = messages.asStateFlow()

        override suspend fun sendMessage(matchId: MatchId, text: String): Message {
            sendCount++
            sendError?.let { throw it }
            return Message(
                id = MessageId("sent-$sendCount"),
                matchId = matchId,
                fromOwnerId = "me",
                text = text,
                createdAt = Instant.fromEpochSeconds(3_000),
                readBy = emptyMap(),
            )
        }

        override suspend fun markAllRead(matchId: MatchId) { markedReadCount++ }

        override suspend fun blockOther(matchId: MatchId) {
            blockError?.let { throw it }
        }

        override suspend fun reportOther(matchId: MatchId, reason: ReportReason, freeText: String?) {
            reportCount++
            lastReport = reason to freeText
            reportError?.let { throw it }
        }
    }

    /** Suspends inside reportOther on a caller-provided gate so the test can observe in-flight state. */
    private class SuspendingReportChatRepo(
        private val gate: kotlinx.coroutines.CompletableDeferred<Unit>,
    ) : ChatRepository {
        val messages = MutableStateFlow<List<Message>>(emptyList())
        var reportCount = 0
        override fun observeChat(matchId: MatchId, pageSize: Int): Flow<List<Message>> = messages.asStateFlow()
        override suspend fun sendMessage(matchId: MatchId, text: String): Message = error("not used")
        override suspend fun markAllRead(matchId: MatchId) = Unit
        override suspend fun blockOther(matchId: MatchId) = Unit
        override suspend fun reportOther(matchId: MatchId, reason: ReportReason, freeText: String?) {
            reportCount++
            gate.await()
        }
    }

    private class FakeMatchRepo(match: Match?) : MatchRepository {
        val matches = MutableStateFlow(match)
        override fun observeMatch(id: MatchId): Flow<Match?> = matches.asStateFlow()
        override fun observeInbox(): Flow<List<Match>> = MutableStateFlow(emptyList<Match>()).asStateFlow()
        override suspend fun unmatch(id: MatchId) { matches.value = null }
    }

    private class FakeOwnerLookup(vararg pairs: Pair<String, OwnerDisplay>) : OwnerLookupRepository {
        private val map = pairs.toMap()
        override fun observeAll(): Flow<Map<String, OwnerDisplay>> = MutableStateFlow(map).asStateFlow()
        override fun observe(ownerId: String): Flow<OwnerDisplay?> = MutableStateFlow(map[ownerId]).asStateFlow()
    }

    private class FakePetLookup(vararg pairs: Pair<String, PetDisplay>) : PetLookupRepository {
        private val map = pairs.toMap()
        override fun observeAll(): Flow<Map<String, PetDisplay>> = MutableStateFlow(map).asStateFlow()
        override fun observe(petId: String): Flow<PetDisplay?> = MutableStateFlow(map[petId]).asStateFlow()
    }

    private class FakeSession(private val session: Session?) : SessionRepository {
        override fun observe(): Flow<Session?> = MutableStateFlow(session).asStateFlow()
        override suspend fun current(): Session? = session
        override suspend fun save(session: Session) = Unit
        override suspend fun clear() = Unit
    }
}
