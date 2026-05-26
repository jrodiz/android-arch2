package com.rodiz.arch2.feature.settings.presentation

import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsState
import com.rodiz.arch2.core.featuredpets.domain.MAX_FEATURED_PETS
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import com.rodiz.arch2.core.testing.MainDispatcherExtension
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class FeaturedPetsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `combines myPets with featured into per-row isFeatured flags`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1"), pet("p2"), pet("p3")))
        val featured = FakeFeaturedRepo()
        featured.state.value = FeaturedPetsState(listOf(FeaturedPet("p1", "Rex"), FeaturedPet("p3", "Bun")))
        val vm = newViewModel(petRepo, featured)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.featuredCount)
        assertEquals(
            mapOf("p1" to true, "p2" to false, "p3" to true),
            state.pets.associate { it.pet.id.value to it.isFeatured },
        )
    }

    @Test
    fun `atLimit true at MAX, false below`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1"), pet("p2"), pet("p3"), pet("p4")))
        val featured = FakeFeaturedRepo()
        val vm = newViewModel(petRepo, featured)
        featured.state.value = FeaturedPetsState(
            (1..MAX_FEATURED_PETS).map { FeaturedPet("p$it", "Pet $it") },
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.atLimit)

        featured.state.value = FeaturedPetsState(listOf(FeaturedPet("p1", "Pet 1")))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.atLimit)
    }

    @Test
    fun `onToggle pins an unfeatured pet`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1")))
        val featured = FakeFeaturedRepo()
        val vm = newViewModel(petRepo, featured)
        advanceUntilIdle()
        vm.onToggle(pet("p1"))
        advanceUntilIdle()
        assertEquals(setOf("p1"), featured.state.value.featured.map { it.id }.toSet())
    }

    @Test
    fun `onToggle unpins a featured pet`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1")))
        val featured = FakeFeaturedRepo()
        featured.state.value = FeaturedPetsState(listOf(FeaturedPet("p1", "Rex")))
        val vm = newViewModel(petRepo, featured)
        advanceUntilIdle()
        vm.onToggle(pet("p1"))
        advanceUntilIdle()
        assertTrue(featured.state.value.featured.isEmpty())
    }

    @Test
    fun `onToggle at limit surfaces snackbar resource`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1"), pet("p2"), pet("p3"), pet("p4")))
        val featured = FakeFeaturedRepo(pinAccepts = { false })
        val vm = newViewModel(petRepo, featured)
        advanceUntilIdle()
        vm.onToggle(pet("p4"))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.snackbarMessageRes)
        vm.onSnackbarShown()
        assertNull(vm.uiState.value.snackbarMessageRes)
    }

    @Test
    fun `first emission triggers refreshFrom once`() = runTest(testDispatcher) {
        val petRepo = FakePetRepo(listOf(pet("p1")))
        val featured = FakeFeaturedRepo()
        newViewModel(petRepo, featured)
        advanceUntilIdle()
        assertEquals(1, featured.refreshCallCount)
        petRepo.pets.value = listOf(pet("p1"), pet("p2"))
        advanceUntilIdle()
        assertEquals(1, featured.refreshCallCount)
    }

    // --- helpers ---

    private fun newViewModel(
        petRepo: PetRepository,
        featuredRepo: FeaturedPetsRepository,
    ): FeaturedPetsViewModel = FeaturedPetsViewModel(
        observeMyPets = ObserveMyPetsUseCase(petRepo),
        featuredRepo = featuredRepo,
    )

    private fun pet(id: String): Pet = Pet(
        id = PetId(id),
        ownerId = "owner",
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

    private class FakePetRepo(initial: List<Pet>) : PetRepository {
        val pets = MutableStateFlow(initial)
        override fun observeMyPets(): Flow<List<Pet>> = pets.asStateFlow()
        override fun observeAllActivePets(limit: Long): Flow<List<Pet>> = pets.asStateFlow()
        override suspend fun currentOwnerHasActivePet(): Boolean = pets.value.any { it.state == PetState.ACTIVE }
        override fun observePet(id: PetId): Flow<Pet?> = MutableStateFlow(pets.value.firstOrNull { it.id == id })
        override suspend fun addPet(draft: PetDraft): Pet = error("not used")
        override suspend fun updatePet(id: PetId, draft: PetDraft): Pet = error("not used")
        override suspend fun archivePet(id: PetId) = Unit
        override suspend fun restorePet(id: PetId) = Unit
        override suspend fun setPetEnabled(id: PetId, enabled: Boolean) = Unit
    }

    private class FakeFeaturedRepo(
        private val pinAccepts: (FeaturedPet) -> Boolean = { true },
    ) : FeaturedPetsRepository {
        val state = MutableStateFlow(FeaturedPetsState())
        var refreshCallCount = 0
        override fun observe(): Flow<FeaturedPetsState> = state.asStateFlow()
        override suspend fun current(): FeaturedPetsState = state.value
        override suspend fun pin(pet: FeaturedPet): Boolean {
            if (!pinAccepts(pet)) return false
            if (state.value.featured.size >= MAX_FEATURED_PETS) return false
            state.value = FeaturedPetsState(state.value.featured + pet)
            return true
        }
        override suspend fun unpin(petId: String) {
            state.value = FeaturedPetsState(state.value.featured.filterNot { it.id == petId })
        }
        override suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>) { refreshCallCount++ }
        override suspend fun wipe() { state.value = FeaturedPetsState() }
        override suspend fun onUserActive(userId: String): UserChangeResult = UserChangeResult.SAME_USER
    }
}
