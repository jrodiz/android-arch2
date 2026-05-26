package com.rodiz.arch2.feature.settings.presentation

import app.cash.turbine.test
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.model.MatchSummary
import com.rodiz.arch2.feature.match.domain.repository.MatchRepository
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.Species
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import com.rodiz.arch2.feature.settings.domain.usecase.CancelAccountDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.ObservePendingDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.RequestAccountDeletionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
internal class DeleteAccountSheetViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `canSubmit is false when typed is not DELETE`() = runTest(testDispatcher) {
        val vm = newViewModel()
        assertEquals("", vm.state.value.typed)
        assertFalse(vm.state.value.canSubmit)
        vm.onTypedChanged("DELET")
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun `onTypedChanged normalizes to uppercase`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onTypedChanged("delete")
        assertEquals("DELETE", vm.state.value.typed)
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `canSubmit blocks while isSubmitting even with DELETE typed`() = runTest(testDispatcher) {
        // Long-running deletion: the use case suspends so isSubmitting stays true
        // long enough that we can assert canSubmit was flipped to false during it.
        val requestDeletion = mockk<RequestAccountDeletionUseCase>()
        val gate = kotlinx.coroutines.CompletableDeferred<AccountDeletion>()
        coEvery { requestDeletion() } coAnswers { gate.await() }
        val sessionRepo = mockk<SessionRepository>(relaxed = true)

        val vm = newViewModel(requestDeletion = requestDeletion, sessionRepo = sessionRepo)
        vm.onTypedChanged("DELETE")
        assertTrue(vm.state.value.canSubmit)

        vm.onConfirmDelete()
        advanceUntilIdle()
        assertTrue(vm.state.value.isSubmitting)
        assertFalse(vm.state.value.canSubmit)

        // Let the use case finish so the test doesn't leak the suspension.
        gate.complete(AccountDeletion(requestedAt = Clock.System.now(), hardDeleteAt = Clock.System.now()))
        advanceUntilIdle()
    }

    @Test
    fun `onConfirmDelete success completes flow and clears session`() = runTest(testDispatcher) {
        val requestDeletion = mockk<RequestAccountDeletionUseCase>()
        coEvery { requestDeletion() } returns AccountDeletion(
            requestedAt = Clock.System.now(),
            hardDeleteAt = Clock.System.now(),
        )
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.clear() } returns Unit

        val vm = newViewModel(requestDeletion = requestDeletion, sessionRepo = sessionRepo)
        vm.onTypedChanged("DELETE")
        vm.state.test {
            // Drop the initial state.
            skipItems(1)
            vm.onConfirmDelete()
            // submitting=true → submitting=false+completed=true
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.state.value.isSubmitting)
        assertTrue(vm.state.value.completed)
        coVerify(exactly = 1) { sessionRepo.clear() }
    }

    @Test
    fun `onConfirmDelete failure surfaces errorRes and unblocks submitting`() = runTest(testDispatcher) {
        val requestDeletion = mockk<RequestAccountDeletionUseCase>()
        coEvery { requestDeletion() } throws RuntimeException("boom")
        val vm = newViewModel(requestDeletion = requestDeletion)
        vm.onTypedChanged("DELETE")
        vm.onConfirmDelete()
        advanceUntilIdle()
        assertFalse(vm.state.value.isSubmitting)
        assertNotNull(vm.state.value.errorRes)
        assertFalse(vm.state.value.completed)
    }

    @Test
    fun `onCancelDeletion calls the cancel use case`() = runTest(testDispatcher) {
        val cancelDeletion = mockk<CancelAccountDeletionUseCase>(relaxed = true)
        coEvery { cancelDeletion() } returns Unit
        val vm = newViewModel(cancelDeletion = cancelDeletion)
        vm.onCancelDeletion()
        advanceUntilIdle()
        coVerify(exactly = 1) { cancelDeletion() }
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `onSheetOpen loads pet names and match count`() = runTest(testDispatcher) {
        val petRepo = mockk<PetRepository>()
        coEvery { petRepo.observeMyPets() } returns flowOf(
            listOf(stubPet("Biscuit"), stubPet("Pearl")),
        )
        val matchRepo = mockk<MatchRepository>()
        coEvery { matchRepo.observeInbox() } returns flowOf(
            listOf(stubMatch("u1"), stubMatch("u2"), stubMatch("u3")),
        )
        // OwnerLookup re-used by ObserveInboxUseCase — give it an empty map; the
        // count formula doesn't depend on owner display data.
        val ownerLookup = mockk<com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository>()
        coEvery { ownerLookup.observeAll() } returns flowOf(emptyMap())
        val petLookup = mockk<com.rodiz.arch2.core.petlookup.domain.PetLookupRepository>()
        coEvery { petLookup.observeAll() } returns flowOf(emptyMap())
        val sessionRepo = mockk<SessionRepository>(relaxed = true)
        coEvery { sessionRepo.current() } returns Session(userId = "me", token = "t")

        val vm = newViewModel(
            sessionRepo = sessionRepo,
            observeMyPets = ObserveMyPetsUseCase(petRepo),
            observeInbox = ObserveInboxUseCase(matchRepo, ownerLookup, petLookup),
        )
        vm.onSheetOpen()
        advanceUntilIdle()
        val snap = vm.state.value
        assertEquals(listOf("Biscuit", "Pearl"), snap.petNames)
        assertEquals(3, snap.matchCount)
    }

    @Test
    fun `pending deletion flows into state from observePendingDeletion`() = runTest(testDispatcher) {
        val pending = MutableStateFlow<AccountDeletion?>(null)
        val pendingRepo = mockk<AccountDeletionRepository>()
        coEvery { pendingRepo.observePendingDeletion() } returns pending

        val vm = newViewModel(observePendingDeletion = ObservePendingDeletionUseCase(pendingRepo))
        advanceUntilIdle()
        assertNull(vm.state.value.pendingDeletion)

        val now = Clock.System.now()
        pending.value = AccountDeletion(requestedAt = now, hardDeleteAt = now)
        advanceUntilIdle()
        assertNotNull(vm.state.value.pendingDeletion)
    }

    // ----- Test helpers --------------------------------------------------------

    private fun newViewModel(
        observePendingDeletion: ObservePendingDeletionUseCase = ObservePendingDeletionUseCase(
            mockk<AccountDeletionRepository>().also { coEvery { it.observePendingDeletion() } returns flowOf(null) },
        ),
        requestDeletion: RequestAccountDeletionUseCase = mockk(relaxed = true),
        cancelDeletion: CancelAccountDeletionUseCase = mockk(relaxed = true),
        sessionRepo: SessionRepository = mockk(relaxed = true) {
            coEvery { current() } returns Session(userId = "me", token = "t")
        },
        observeMyPets: ObserveMyPetsUseCase = ObserveMyPetsUseCase(
            mockk<PetRepository>().also { coEvery { it.observeMyPets() } returns flowOf(emptyList()) },
        ),
        observeInbox: ObserveInboxUseCase = ObserveInboxUseCase(
            mockk<MatchRepository>().also { coEvery { it.observeInbox() } returns flowOf(emptyList()) },
            mockk<com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository>().also {
                coEvery { it.observeAll() } returns flowOf(emptyMap())
            },
            mockk<com.rodiz.arch2.core.petlookup.domain.PetLookupRepository>().also {
                coEvery { it.observeAll() } returns flowOf(emptyMap())
            },
        ),
    ): DeleteAccountSheetViewModel = DeleteAccountSheetViewModel(
        observePendingDeletion = observePendingDeletion,
        requestDeletion = requestDeletion,
        cancelDeletion = cancelDeletion,
        sessionRepository = sessionRepo,
        observeMyPets = observeMyPets,
        observeInbox = observeInbox,
    )

    private fun stubPet(name: String): Pet = Pet(
        id = PetId("pet-$name"),
        ownerId = "me",
        name = name,
        ageYears = 3,
        ageIsApproximate = false,
        species = Species.DOG,
        intents = setOf(Intent.PLAYDATE),
        photos = emptyList(),
        bio = null,
        state = PetState.ACTIVE,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        deletedAt = null,
    )

    private fun stubMatch(otherUid: String): Match = Match(
        id = MatchId("match-$otherUid"),
        ownerAId = "me",
        ownerBId = otherUid,
        createdAt = Clock.System.now(),
        lastMessageAt = null,
        lastMessagePreview = null,
        lastMessageFromOwnerId = null,
    )

    @Suppress("unused") // kept for documentation; ObserveInboxUseCase consumes Match directly
    private fun stubSummary(otherUid: String): MatchSummary = MatchSummary(
        match = stubMatch(otherUid),
        otherOwnerId = otherUid,
    )
}
