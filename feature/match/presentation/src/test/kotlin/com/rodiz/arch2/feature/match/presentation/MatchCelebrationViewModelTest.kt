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
import com.rodiz.arch2.feature.match.domain.usecase.ObserveMatchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class MatchCelebrationViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    private val meUid = "me"
    private val themUid = "them"
    private val myPetId = "my-pet"
    private val theirPetId = "their-pet"
    private val matchIdValue = "m1"

    @Test
    fun `initial state is not ready and has no errors`() = runTest(testDispatcher) {
        val vm = newViewModel(matchRepo = FakeMatchRepo(match = null))
        val state = vm.uiState.value
        assertFalse(state.isReady)
        assertNull(state.errorMessage)
        assertNull(state.match)
    }

    @Test
    fun `all lookups populated flips isReady true`() = runTest(testDispatcher) {
        val petLookup = FakePetLookup(
            myPetId to PetDisplay(id = myPetId, ownerId = meUid, name = "Biscuit", species = "Dog", avatarUrl = null),
            theirPetId to PetDisplay(id = theirPetId, ownerId = themUid, name = "Mochi", species = "Cat", avatarUrl = null),
        )
        val ownerLookup = FakeOwnerLookup(
            meUid to OwnerDisplay(id = meUid, firstName = "Joe", avatarUrl = null),
            themUid to OwnerDisplay(id = themUid, firstName = "Sam", avatarUrl = null),
        )
        val vm = newViewModel(
            matchRepo = FakeMatchRepo(match = matchOf()),
            petLookup = petLookup,
            ownerLookup = ownerLookup,
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.isReady)
        assertEquals("Biscuit", state.myPet?.name)
        assertEquals("Mochi", state.theirPet?.name)
        assertEquals("Joe", state.me?.firstName)
        assertEquals("Sam", state.them?.firstName)
    }

    @Test
    fun `routes myPetId and theirPetId correctly when current user is ownerA`() = runTest(testDispatcher) {
        // I'm ownerA → my pet is petAId, their pet is petBId.
        val petLookup = FakePetLookup(
            myPetId to PetDisplay(id = myPetId, ownerId = meUid, name = "Mine", species = null, avatarUrl = null),
            theirPetId to PetDisplay(id = theirPetId, ownerId = themUid, name = "Theirs", species = null, avatarUrl = null),
        )
        val vm = newViewModel(
            matchRepo = FakeMatchRepo(match = matchOf(ownerAId = meUid, ownerBId = themUid)),
            petLookup = petLookup,
        )
        advanceUntilIdle()
        assertEquals("Mine", vm.uiState.value.myPet?.name)
        assertEquals("Theirs", vm.uiState.value.theirPet?.name)
    }

    @Test
    fun `routes myPetId and theirPetId correctly when current user is ownerB`() = runTest(testDispatcher) {
        // Flip sides — I'm ownerB → my pet is petBId, their pet is petAId.
        val petLookup = FakePetLookup(
            myPetId to PetDisplay(id = myPetId, ownerId = meUid, name = "Mine", species = null, avatarUrl = null),
            theirPetId to PetDisplay(id = theirPetId, ownerId = themUid, name = "Theirs", species = null, avatarUrl = null),
        )
        val vm = newViewModel(
            matchRepo = FakeMatchRepo(
                // ownerA = them, ownerB = me; pet ids swap to match.
                match = matchOf(ownerAId = themUid, ownerBId = meUid, petAId = theirPetId, petBId = myPetId),
            ),
            petLookup = petLookup,
        )
        advanceUntilIdle()
        assertEquals("Mine", vm.uiState.value.myPet?.name)
        assertEquals("Theirs", vm.uiState.value.theirPet?.name)
    }

    @Test
    fun `null theirPet keeps isReady false but does not crash`() = runTest(testDispatcher) {
        // Pet lookup returns null for theirPet — Firestore doc missing.
        val petLookup = FakePetLookup(
            myPetId to PetDisplay(id = myPetId, ownerId = meUid, name = "Biscuit", species = null, avatarUrl = null),
        )
        val vm = newViewModel(
            matchRepo = FakeMatchRepo(match = matchOf()),
            petLookup = petLookup,
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertNotNull(state.match)
        assertEquals("Biscuit", state.myPet?.name)
        assertNull(state.theirPet)
        assertFalse(state.isReady)
    }

    @Test
    fun `null match keeps isReady false and does not trigger lookups`() = runTest(testDispatcher) {
        val petLookup = RecordingPetLookup()
        val vm = newViewModel(
            matchRepo = FakeMatchRepo(match = null),
            petLookup = petLookup,
        )
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isReady)
        assertEquals(0, petLookup.observeCalls.size)
    }

    @Test
    fun `no signed-in user surfaces errorMessage`() = runTest(testDispatcher) {
        val vm = newViewModel(session = FakeSession(session = null))
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("Sign-in required", state.errorMessage)
        assertFalse(state.isReady)
    }

    // ---- helpers ----

    private fun newViewModel(
        matchRepo: MatchRepository = FakeMatchRepo(match = matchOf()),
        petLookup: PetLookupRepository = FakePetLookup(),
        ownerLookup: OwnerLookupRepository = FakeOwnerLookup(),
        session: SessionRepository = FakeSession(Session(userId = meUid, token = "t")),
    ): MatchCelebrationViewModel = MatchCelebrationViewModel(
        matchIdValue = matchIdValue,
        sessionRepo = session,
        observeMatch = ObserveMatchUseCase(matchRepo),
        petLookup = petLookup,
        ownerLookup = ownerLookup,
    )

    private fun matchOf(
        ownerAId: String = meUid,
        ownerBId: String = themUid,
        petAId: String? = myPetId,
        petBId: String? = theirPetId,
    ): Match = Match(
        id = MatchId(matchIdValue),
        ownerAId = ownerAId,
        ownerBId = ownerBId,
        createdAt = Instant.fromEpochSeconds(1_000),
        lastMessageAt = null,
        lastMessagePreview = null,
        lastMessageFromOwnerId = null,
        petAId = petAId,
        petBId = petBId,
    )

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

    private open class FakePetLookup(vararg pairs: Pair<String, PetDisplay>) : PetLookupRepository {
        protected val map = pairs.toMap()
        override fun observeAll(): Flow<Map<String, PetDisplay>> = MutableStateFlow(map).asStateFlow()
        override fun observe(petId: String): Flow<PetDisplay?> = MutableStateFlow(map[petId]).asStateFlow()
    }

    /** Tracks observe() invocations so the "match==null skips lookups" case can assert no calls were made. */
    private class RecordingPetLookup : FakePetLookup() {
        val observeCalls = mutableListOf<String>()
        override fun observe(petId: String): Flow<PetDisplay?> {
            observeCalls += petId
            return super.observe(petId)
        }
    }

    private class FakeSession(private val session: Session?) : SessionRepository {
        override fun observe(): Flow<Session?> = MutableStateFlow(session).asStateFlow()
        override suspend fun current(): Session? = session
        override suspend fun save(session: Session) = Unit
        override suspend fun clear() = Unit
    }
}
