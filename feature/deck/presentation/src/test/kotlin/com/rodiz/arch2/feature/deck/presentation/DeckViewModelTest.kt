package com.rodiz.arch2.feature.deck.presentation

import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.core.filters.domain.FilterPrefsRepository
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.deck.domain.usecase.ObserveDeckUseCase
import com.rodiz.arch2.feature.deck.domain.usecase.ReviewPassedPetsUseCase
import com.rodiz.arch2.feature.deck.domain.usecase.SubmitSwipeUseCase
import com.rodiz.arch2.feature.deck.domain.usecase.UndoLastSwipeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.Species
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `initial state defaults to EXHAUSTED + hasOwnPet=true so banners don't flash`() = runTest(testDispatcher) {
        val vm = newViewModel()
        val state = vm.uiState.value
        assertTrue(state.cards.isEmpty())
        assertEquals(DeckState.EXHAUSTED, state.state)
        assertTrue(state.hasOwnPet, "default-true avoids the add-a-pet banner flashing before My Pets loads")
    }

    @Test
    fun `deck snapshot drives cards into UiState`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo()
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.cards.isEmpty(), "starts empty until a snapshot lands")
        deck.snapshots.value = DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY)
        advanceUntilIdle()
        val ready = vm.uiState.value
        assertEquals(listOf("p1", "p2"), ready.cards.map { it.pet.id.value })
        assertEquals(DeckState.READY, ready.state)
    }

    @Test
    fun `READY snapshot with empty cards is coerced to EXHAUSTED`() = runTest(testDispatcher) {
        // The VM downgrades READY+empty to EXHAUSTED so the empty-state UI renders
        // immediately instead of holding a spinner while the user has nothing to swipe.
        val deck = FakeDeckRepo(DeckSnapshot(cards = emptyList(), state = DeckState.READY))
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        assertEquals(DeckState.EXHAUSTED, vm.uiState.value.state)
    }

    @Test
    fun `likeTop optimistically removes top card and reports match`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY))
        deck.nextSwipeResult = SwipeResult.Match(matchId = "m1")
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()

        vm.likeTop()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("p2"), state.cards.map { it.pet.id.value }, "top card removed optimistically")
        // The matchId from SwipeResult is handed up to the Route via pendingMatchId
        // so it can navigate to the celebration screen.
        assertEquals("m1", state.pendingMatchId)
        assertEquals(PetId("p1") to SwipeAction.LIKE, deck.lastSwipe)
    }

    @Test
    fun `passTop surfaces RequiresPet message when domain reports it`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1")), state = DeckState.READY))
        deck.nextSwipeResult = SwipeResult.RequiresPet
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()

        vm.passTop()
        advanceUntilIdle()

        // RequiresPet flips the dialog flag; the screen renders the modal copy
        // via stringResource so we don't assert on text here.
        assertEquals(true, vm.uiState.value.requiresPetDialog)
    }

    @Test
    fun `swipe failure surfaces errorMessage and leaves top card removed`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY))
        deck.swipeError = RuntimeException("offline")
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()

        vm.likeTop()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("offline", state.errorMessage)
        // Optimistic remove is intentional even on failure — refreshing the snapshot
        // is what restores the card. We don't want a UI bounce on transient errors.
        assertEquals(listOf("p2"), state.cards.map { it.pet.id.value })
    }

    @Test
    fun `swiped pet is filtered out of subsequent snapshots`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY))
        deck.nextSwipeResult = SwipeResult.Pending
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        vm.passTop()
        advanceUntilIdle()

        // Backend re-emits the same set (e.g. snapshot listener firing). The VM should
        // drop p1 client-side so we don't briefly re-show the card we just swiped.
        deck.snapshots.value = DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY)
        advanceUntilIdle()

        assertEquals(listOf("p2"), vm.uiState.value.cards.map { it.pet.id.value })
    }

    @Test
    fun `rewind re-prepends the un-swiped pet`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY))
        deck.nextSwipeResult = SwipeResult.Pending
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        vm.passTop()
        advanceUntilIdle()

        deck.nextUndo = pet("p1")
        vm.rewind()
        advanceUntilIdle()

        val cards = vm.uiState.value.cards.map { it.pet.id.value }
        assertEquals(listOf("p1", "p2"), cards)
        // Once rewound, a fresh snapshot containing p1 should also be allowed through.
        deck.snapshots.value = DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY)
        advanceUntilIdle()
        assertEquals(listOf("p1", "p2"), vm.uiState.value.cards.map { it.pet.id.value })
    }

    @Test
    fun `rewind is a no-op when undo returns null`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1")), state = DeckState.READY))
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        val before = vm.uiState.value.cards.map { it.pet.id.value }
        vm.rewind()
        advanceUntilIdle()
        assertEquals(before, vm.uiState.value.cards.map { it.pet.id.value })
    }

    @Test
    fun `like with no pet shows RequiresPet dialog and keeps the card on the deck`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo(
                DeckSnapshot(cards = listOf(card("p1"), card("p2")), state = DeckState.READY),
            )
            deck.nextSwipeResult = SwipeResult.RequiresPet
            val vm = newViewModel(deckRepo = deck)
            advanceUntilIdle()

            vm.likeTop()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.requiresPetDialog, "rejected like must surface the add-a-pet dialog")
            // The like was never recorded (no pet) — the card must be restored, not consumed,
            // so the user can like it again after adding a pet.
            assertEquals(listOf("p1", "p2"), state.cards.map { it.pet.id.value })
        }

    @Test
    fun `reviewPasses re-subscribes the deck so restored pets reappear without a new snapshot`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1")), state = DeckState.READY))
            deck.nextSwipeResult = SwipeResult.Pending
            deck.nextClearTodayCount = 1
            val vm = newViewModel(deckRepo = deck)
            advanceUntilIdle()
            vm.passTop()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.cards.isEmpty(), "p1 is filtered out after the pass")

            // No manual snapshot push here: reviewPasses must itself re-run observeDeck so the
            // same snapshot (still holding p1, whose pass we just cleared) flows back in. Without
            // the refresh trigger the deck would be stuck on its LOADING spinner.
            vm.reviewPasses()
            advanceUntilIdle()

            assertEquals(listOf("p1"), vm.uiState.value.cards.map { it.pet.id.value })
            assertEquals(DeckState.READY, vm.uiState.value.state)
        }

    @Test
    fun `filter prefs flow into meta strip counts`() = runTest(testDispatcher) {
        val prefs = FakeFilterPrefsRepo(
            FilterPrefs(
                maxDistanceKm = 10,
                intents = setOf(Intent.PLAYDATE),
                species = setOf(Species.DOG, Species.CAT, Species.RABBIT),
            ),
        )
        val vm = newViewModel(filterRepo = prefs)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(10, state.maxDistanceKm)
        assertEquals(1, state.intentsCount)
        // Species count collapses to its category set so the header icon strip stays
        // at 1–3 paws even though the underlying species list is granular.
        assertEquals(setOf(Species.DOG, Species.CAT, Species.RABBIT).map { it.category }.toSet().size, state.speciesCount)
    }

    @Test
    fun `My Pets snapshot flips hasOwnPet to false when user has no active pet`() = runTest(testDispatcher) {
        val pets = FakePetRepo()
        val vm = newViewModel(petRepo = pets)
        pets.myPets.value = emptyList()
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.hasOwnPet)
        pets.myPets.value = listOf(pet("mine", state = PetState.ACTIVE))
        advanceUntilIdle()
        assertEquals(true, vm.uiState.value.hasOwnPet)
    }

    @Test
    fun `clearMatch + clearRequiresPet + clearError reset their fields`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1")), state = DeckState.READY))
        deck.nextSwipeResult = SwipeResult.Match("m1")
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        vm.likeTop()
        advanceUntilIdle()

        vm.clearMatch()
        vm.clearRequiresPet()
        vm.clearError()
        val cleared = vm.uiState.value
        assertNull(cleared.pendingMatchId)
        assertEquals(false, cleared.requiresPetDialog)
        assertNull(cleared.errorMessage)
    }

    @Test
    fun `reviewPasses success sets count and lets restored pets pass the swipe filter`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo(DeckSnapshot(cards = listOf(card("p1")), state = DeckState.READY))
            deck.nextClearTodayCount = 14
            val vm = newViewModel(deckRepo = deck)
            advanceUntilIdle()
            // Pre-pass a card so recentlySwiped has an entry to clear.
            vm.passTop()
            advanceUntilIdle()

            vm.reviewPasses()
            advanceUntilIdle()

            assertEquals(14, vm.uiState.value.reviewedPassesCount)
            assertEquals(1, deck.clearTodayCalls)
            // New emission containing the previously-passed pet p1 — it should pass the
            // recentlySwiped filter (which reviewPasses just cleared) and reach uiState.
            // Adding p2 forces StateFlow to actually re-emit (dedup-safe).
            deck.snapshots.value = DeckSnapshot(
                cards = listOf(card("p1"), card("p2")),
                state = DeckState.READY,
            )
            advanceUntilIdle()
            assertEquals(listOf("p1", "p2"), vm.uiState.value.cards.map { it.pet.id.value })
        }

    @Test
    fun `reviewPasses failure surfaces errorMessage and leaves count null`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo()
            deck.clearTodayError = RuntimeException("boom")
            val vm = newViewModel(deckRepo = deck)
            advanceUntilIdle()

            vm.reviewPasses()
            advanceUntilIdle()

            assertEquals("boom", vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.reviewedPassesCount)
        }

    @Test
    fun `clearReviewMessage clears the count`() = runTest(testDispatcher) {
        val deck = FakeDeckRepo().apply { nextClearTodayCount = 3 }
        val vm = newViewModel(deckRepo = deck)
        advanceUntilIdle()
        vm.reviewPasses()
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.reviewedPassesCount)

        vm.clearReviewMessage()
        assertNull(vm.uiState.value.reviewedPassesCount)
    }

    // ---- helpers ----

    private fun newViewModel(
        deckRepo: DeckRepository = FakeDeckRepo(),
        petRepo: PetRepository = FakePetRepo(),
        filterRepo: FilterPrefsRepository = FakeFilterPrefsRepo(),
    ): DeckViewModel = DeckViewModel(
        observeDeck = ObserveDeckUseCase(deckRepo),
        observeMyPets = ObserveMyPetsUseCase(petRepo),
        filterPrefsRepo = filterRepo,
        submitSwipe = SubmitSwipeUseCase(deckRepo),
        undoLastSwipe = UndoLastSwipeUseCase(deckRepo),
        reviewPassedPets = ReviewPassedPetsUseCase(deckRepo),
    )

    private fun card(id: String): DeckCard = DeckCard(pet = pet(id))

    private fun pet(id: String, state: PetState = PetState.ACTIVE): Pet = Pet(
        id = PetId(id),
        ownerId = "owner-$id",
        name = "Pet $id",
        ageYears = 2,
        ageIsApproximate = false,
        species = Species.DOG,
        intents = setOf(Intent.PLAYDATE),
        photos = emptyList(),
        bio = null,
        state = state,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        deletedAt = null,
    )

    private class FakeDeckRepo(
        initial: DeckSnapshot = DeckSnapshot(cards = emptyList(), state = DeckState.EXHAUSTED),
    ) : DeckRepository {
        val snapshots = MutableStateFlow(initial)
        var nextSwipeResult: SwipeResult = SwipeResult.Pending
        var swipeError: Throwable? = null
        var nextUndo: Pet? = null
        var lastSwipe: Pair<PetId, SwipeAction>? = null
        var nextClearTodayCount: Int = 0
        var clearTodayError: Throwable? = null
        var clearTodayCalls: Int = 0

        override fun observeDeck(filters: FilterPrefs): Flow<DeckSnapshot> = snapshots.asStateFlow()

        override suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult {
            lastSwipe = petId to action
            swipeError?.let { throw it }
            return nextSwipeResult
        }

        override suspend fun undoLastSwipe(): Pet? = nextUndo

        override suspend fun clearTodayPasses(): Int {
            clearTodayCalls += 1
            clearTodayError?.let { throw it }
            return nextClearTodayCount
        }
    }

    private class FakePetRepo(initial: List<Pet> = emptyList()) : PetRepository {
        val myPets = MutableStateFlow(initial)
        override fun observeMyPets(): Flow<List<Pet>> = myPets.asStateFlow()
        override fun observeAllActivePets(limit: Long): Flow<List<Pet>> = myPets.asStateFlow()
        override suspend fun currentOwnerHasActivePet(): Boolean = myPets.value.any { it.state == PetState.ACTIVE }
        override fun observePet(id: PetId): Flow<Pet?> = MutableStateFlow(myPets.value.firstOrNull { it.id == id })
        override suspend fun addPet(draft: PetDraft): Pet = error("not used in DeckViewModelTest")
        override suspend fun updatePet(id: PetId, draft: PetDraft): Pet = error("not used in DeckViewModelTest")
        override suspend fun archivePet(id: PetId) = Unit
        override suspend fun restorePet(id: PetId) = Unit
        override suspend fun setPetEnabled(id: PetId, enabled: Boolean) = Unit
    }

    private class FakeFilterPrefsRepo(initial: FilterPrefs = FilterPrefs.DEFAULT) : FilterPrefsRepository {
        val prefs = MutableStateFlow(initial)
        override fun observePrefs(): Flow<FilterPrefs> = prefs.asStateFlow()
        override suspend fun updatePrefs(prefs: FilterPrefs) { this.prefs.value = prefs }
    }
}
