package com.rodiz.arch2.feature.likes.presentation

import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import com.rodiz.arch2.feature.likes.domain.repository.LikesYouRepository
import com.rodiz.arch2.feature.likes.domain.usecase.LikeBackUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.ObserveLikesYouUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.PassLikeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.Species
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
class LikesYouViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    // D-007: declining a like from the grid must (a) hide the tile optimistically and
    // (b) write the passedLikes doc (via PassLikeUseCase → repo.pass) so it stays gone.
    @Test
    fun `pass hides the like and records the decline`() = runTest(testDispatcher) {
        val like = incomingLike("k1")
        val repo = FakeLikesRepo(listOf(like))
        val vm = LikesYouViewModel(
            observeLikesYou = ObserveLikesYouUseCase(repo),
            passLike = PassLikeUseCase(repo),
            likeBack = LikeBackUseCase(repo),
        )
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.likes.size)

        vm.pass(LikeKey("k1"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.likes.isEmpty(), "declined like must leave the grid")
        assertEquals(listOf(LikeKey("k1")), repo.passed, "decline must write passedLikes")
    }

    @Test
    fun `declined like stays gone even if the flow re-emits it`() = runTest(testDispatcher) {
        val like = incomingLike("k1")
        val repo = FakeLikesRepo(listOf(like))
        val vm = LikesYouViewModel(
            observeLikesYou = ObserveLikesYouUseCase(repo),
            passLike = PassLikeUseCase(repo),
            likeBack = LikeBackUseCase(repo),
        )
        advanceUntilIdle()
        vm.pass(LikeKey("k1"))
        advanceUntilIdle()

        // The data layer hasn't dropped it yet — re-emit the same list. The local hide must hold.
        repo.reemit(listOf(like))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.likes.isEmpty(), "locally-hidden decline must survive a re-emit")
    }

    // ---- helpers ----

    private fun incomingLike(key: String): IncomingLike = IncomingLike(
        key = LikeKey(key),
        fromOwnerId = "owner-$key",
        toPetId = PetId("my-pet"),
        anchorPet = pet("anchor-$key"),
        likedAt = Instant.fromEpochSeconds(1_000),
        distanceBucket = null,
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

    private class FakeLikesRepo(initial: List<IncomingLike>) : LikesYouRepository {
        private val flow = MutableStateFlow(initial)
        val passed = mutableListOf<LikeKey>()
        fun reemit(list: List<IncomingLike>) { flow.value = list }
        override fun observeLikesYou(): Flow<List<IncomingLike>> = flow.asStateFlow()
        override suspend fun pass(key: LikeKey) { passed += key }
        override suspend fun likeBack(key: LikeKey): SwipeResult = SwipeResult.Pending
    }
}
