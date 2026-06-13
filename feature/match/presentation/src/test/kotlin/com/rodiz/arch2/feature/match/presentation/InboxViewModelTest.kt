package com.rodiz.arch2.feature.match.presentation

import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.petlookup.domain.PetDisplay
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    // Regression for D-010: the inbox must re-key when the session changes (in-app account
    // switch). The old one-shot `current()` read left the VM bound to whoever was signed in
    // at init — so a user who signed in later (or a different user after a switch) saw a
    // stale/empty inbox. With reactive `observe()`, a session arriving repopulates it.
    @Test
    fun `inbox reacts to a session arriving after init`() = runTest(testDispatcher) {
        val session = FakeSession(initial = null)
        val repo = FakeMatchRepo(listOf(matchWithMessage()))
        val vm = newViewModel(repo = repo, session = session)
        advanceUntilIdle()

        // No signed-in user yet → ready, but empty.
        assertTrue(vm.uiState.value.isReady)
        assertEquals(0, vm.uiState.value.snapshot.conversations.size)

        // A user signs in → the inbox must populate without re-creating the VM.
        session.emit(Session(userId = "me", token = "t"))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.snapshot.conversations.size)
    }

    @Test
    fun `clearing the session resets the inbox to empty`() = runTest(testDispatcher) {
        val session = FakeSession(initial = Session(userId = "me", token = "t"))
        val repo = FakeMatchRepo(listOf(matchWithMessage()))
        val vm = newViewModel(repo = repo, session = session)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.snapshot.conversations.size)

        // Sign out → the previous user's conversations must not linger on screen.
        session.emit(null)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.snapshot.conversations.size)
    }

    // ---- helpers ----

    private fun newViewModel(
        repo: MatchRepository,
        session: SessionRepository,
    ): InboxViewModel = InboxViewModel(
        observeInbox = ObserveInboxUseCase(repo, FakeOwnerLookup(), FakePetLookup()),
        sessionRepo = session,
    )

    private fun matchWithMessage(): Match = Match(
        id = MatchId("m1"),
        ownerAId = "me",
        ownerBId = "them",
        createdAt = Instant.fromEpochSeconds(1_000),
        lastMessageAt = Instant.fromEpochSeconds(2_000),
        lastMessagePreview = "hi",
        lastMessageFromOwnerId = "them",
        petAId = "pa",
        petBId = "pb",
    )

    private class FakeMatchRepo(matches: List<Match>) : MatchRepository {
        private val inbox = MutableStateFlow(matches)
        override fun observeInbox(): Flow<List<Match>> = inbox.asStateFlow()
        override fun observeMatch(id: MatchId): Flow<Match?> = MutableStateFlow<Match?>(null).asStateFlow()
        override suspend fun unmatch(id: MatchId) = Unit
    }

    private class FakeOwnerLookup : OwnerLookupRepository {
        override fun observeAll(): Flow<Map<String, OwnerDisplay>> = MutableStateFlow(emptyMap<String, OwnerDisplay>()).asStateFlow()
        override fun observe(ownerId: String): Flow<OwnerDisplay?> = MutableStateFlow(null).asStateFlow()
    }

    private class FakePetLookup : PetLookupRepository {
        override fun observeAll(): Flow<Map<String, PetDisplay>> = MutableStateFlow(emptyMap<String, PetDisplay>()).asStateFlow()
        override fun observe(petId: String): Flow<PetDisplay?> = MutableStateFlow(null).asStateFlow()
    }

    private class FakeSession(initial: Session?) : SessionRepository {
        private val flow = MutableStateFlow(initial)
        fun emit(session: Session?) { flow.value = session }
        override fun observe(): Flow<Session?> = flow.asStateFlow()
        override suspend fun current(): Session? = flow.value
        override suspend fun save(session: Session) { flow.value = session }
        override suspend fun clear() { flow.value = null }
    }
}
