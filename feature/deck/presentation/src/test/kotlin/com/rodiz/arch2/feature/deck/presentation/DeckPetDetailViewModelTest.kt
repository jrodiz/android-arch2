package com.rodiz.arch2.feature.deck.presentation

import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.deck.domain.usecase.SubmitSwipeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.Species
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import com.rodiz.arch2.feature.pet.domain.usecase.ObservePetUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class DeckPetDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `like that matches emits MatchOccurred so the route shows the celebration`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo().apply { nextSwipeResult = SwipeResult.Match("m99") }
            val bus = DeckDetailResultBus()
            val vm = newViewModel(deckRepo = deck, bus = bus)
            advanceUntilIdle()

            val events = mutableListOf<DeckPetDetailEvent>()
            val job = launch { vm.events.collect { events.add(it) } }

            vm.like()
            advanceUntilIdle()

            assertEquals(
                listOf<DeckPetDetailEvent>(DeckPetDetailEvent.MatchOccurred("m99")),
                events,
                "a match must navigate to the celebration, not just dismiss",
            )
            job.cancel()
        }

    @Test
    fun `non-match like dismisses and publishes the result to the deck bus`() =
        runTest(testDispatcher) {
            val deck = FakeDeckRepo().apply { nextSwipeResult = SwipeResult.Pending }
            val bus = DeckDetailResultBus()
            val vm = newViewModel(deckRepo = deck, bus = bus)
            advanceUntilIdle()

            val events = mutableListOf<DeckPetDetailEvent>()
            val outcomes = mutableListOf<DeckDetailResultBus.Outcome>()
            val eventsJob = launch { vm.events.collect { events.add(it) } }
            val busJob = launch { bus.outcomes.collect { outcomes.add(it) } }

            vm.pass()
            advanceUntilIdle()

            assertEquals(listOf<DeckPetDetailEvent>(DeckPetDetailEvent.Dismiss), events)
            assertEquals(1, outcomes.size)
            assertEquals(PetId("p1"), outcomes[0].petId)
            assertTrue(outcomes[0].result is SwipeResult.Pending)
            eventsJob.cancel()
            busJob.cancel()
        }

    // ---- helpers ----

    private fun newViewModel(
        deckRepo: DeckRepository = FakeDeckRepo(),
        bus: DeckDetailResultBus = DeckDetailResultBus(),
    ): DeckPetDetailViewModel = DeckPetDetailViewModel(
        petIdValue = "p1",
        observePet = ObservePetUseCase(FakePetRepo()),
        ownerLookup = FakeOwnerLookup(),
        submitSwipe = SubmitSwipeUseCase(deckRepo),
        resultBus = bus,
    )

    private fun pet(id: String): Pet = Pet(
        id = PetId(id),
        ownerId = "owner-$id",
        name = "Pet $id",
        ageYears = 2,
        ageIsApproximate = false,
        species = Species.DOG,
        intents = setOf(Intent.PLAYDATE),
        photos = emptyList(),
        bio = null,
        state = PetState.ACTIVE,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        deletedAt = null,
    )

    private inner class FakePetRepo : PetRepository {
        override fun observeMyPets(): Flow<List<Pet>> = MutableStateFlow(emptyList())
        override fun observeAllActivePets(limit: Long): Flow<List<Pet>> = MutableStateFlow(emptyList())
        override suspend fun currentOwnerHasActivePet(): Boolean = true
        override fun observePet(id: PetId): Flow<Pet?> = MutableStateFlow(pet(id.value))
        override suspend fun addPet(draft: PetDraft): Pet = error("unused")
        override suspend fun updatePet(id: PetId, draft: PetDraft): Pet = error("unused")
        override suspend fun archivePet(id: PetId) = Unit
        override suspend fun restorePet(id: PetId) = Unit
        override suspend fun setPetEnabled(id: PetId, enabled: Boolean) = Unit
    }

    private class FakeOwnerLookup : OwnerLookupRepository {
        override fun observeAll(): Flow<Map<String, OwnerDisplay>> = MutableStateFlow(emptyMap())
        override fun observe(ownerId: String): Flow<OwnerDisplay?> = MutableStateFlow(null)
    }

    private class FakeDeckRepo : DeckRepository {
        val snapshots = MutableStateFlow(DeckSnapshot(cards = emptyList(), state = DeckState.EXHAUSTED))
        var nextSwipeResult: SwipeResult = SwipeResult.Pending
        override fun observeDeck(filters: FilterPrefs): Flow<DeckSnapshot> = snapshots.asStateFlow()
        override suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult = nextSwipeResult
        override suspend fun undoLastSwipe(): Pet? = null
        override suspend fun clearTodayPasses(): Int = 0
    }
}
