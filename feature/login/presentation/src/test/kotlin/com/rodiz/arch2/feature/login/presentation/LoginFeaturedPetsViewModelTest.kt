package com.rodiz.arch2.feature.login.presentation

import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsState
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.login.presentation.viewmodel.LoginFeaturedPetsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class LoginFeaturedPetsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `empty state surfaces an empty list`() = runTest(testDispatcher) {
        val repo = FakeFeaturedRepo()
        val vm = LoginFeaturedPetsViewModel(repo)
        advanceUntilIdle()
        assertEquals(emptyList<FeaturedPet>(), vm.featured.value)
    }

    @Test
    fun `featured pets flow through into StateFlow in order`() = runTest(testDispatcher) {
        val repo = FakeFeaturedRepo()
        val vm = LoginFeaturedPetsViewModel(repo)
        repo.state.value = FeaturedPetsState(
            featured = listOf(
                FeaturedPet("p1", "Rex"),
                FeaturedPet("p2", "Mochi"),
            ),
        )
        advanceUntilIdle()
        assertEquals(listOf("p1", "p2"), vm.featured.value.map { it.id })
    }

    private class FakeFeaturedRepo : FeaturedPetsRepository {
        val state = MutableStateFlow(FeaturedPetsState())
        override fun observe(): Flow<FeaturedPetsState> = state.asStateFlow()
        override suspend fun current(): FeaturedPetsState = state.value
        override suspend fun pin(pet: FeaturedPet): Boolean = true
        override suspend fun unpin(petId: String) = Unit
        override suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>) = Unit
        override suspend fun wipe() { state.value = FeaturedPetsState() }
        override suspend fun onUserActive(userId: String): UserChangeResult = UserChangeResult.SAME_USER
    }
}
