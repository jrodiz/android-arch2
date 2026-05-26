package com.rodiz.arch2.core.featuredpets.data

import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsState
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeaturedPetsSessionGateTest {

    @Test
    fun `null userId preserves cache without consulting the repo`() = runTest {
        val repo = RecordingRepo()
        val gate = FeaturedPetsSessionGate(repo)
        gate.onSessionChanged(null)
        assertEquals(emptyList<String>(), repo.calls)
    }

    @Test
    fun `first sign-in records uid without wipe`() = runTest {
        val repo = RecordingRepo(nextResult = UserChangeResult.FIRST_USER)
        val gate = FeaturedPetsSessionGate(repo)
        gate.onSessionChanged("uidA")
        assertEquals(listOf("onUserActive:uidA"), repo.calls)
    }

    @Test
    fun `same user re-activation does not wipe`() = runTest {
        val repo = RecordingRepo(nextResult = UserChangeResult.SAME_USER)
        val gate = FeaturedPetsSessionGate(repo)
        gate.onSessionChanged("uidA")
        assertEquals(listOf("onUserActive:uidA"), repo.calls)
    }

    @Test
    fun `user change triggers wipe`() = runTest {
        val repo = RecordingRepo(nextResult = UserChangeResult.USER_CHANGED)
        val gate = FeaturedPetsSessionGate(repo)
        gate.onSessionChanged("uidB")
        assertEquals(listOf("onUserActive:uidB", "wipe"), repo.calls)
    }

    @Test
    fun `sign-out between sign-ins preserves cache for the same user`() = runTest {
        // Simulates: sign in as A → sign out (null) → sign in as A again.
        // The intermediate null must NOT trigger a wipe; the second A activation
        // must report SAME_USER and not wipe either.
        val repo = ScriptedRepo(
            listOf(
                UserChangeResult.FIRST_USER,
                UserChangeResult.SAME_USER,
            ),
        )
        val gate = FeaturedPetsSessionGate(repo)
        gate.onSessionChanged("uidA")
        gate.onSessionChanged(null) // sign-out
        gate.onSessionChanged("uidA")
        assertEquals(
            listOf("onUserActive:uidA", "onUserActive:uidA"),
            repo.calls,
        )
    }

    // --- fakes ---

    private open class RecordingRepo(
        private val nextResult: UserChangeResult = UserChangeResult.SAME_USER,
    ) : FeaturedPetsRepository {
        val calls = mutableListOf<String>()
        private val state = MutableStateFlow(FeaturedPetsState())
        override fun observe(): Flow<FeaturedPetsState> = state.asStateFlow()
        override suspend fun current(): FeaturedPetsState = state.value
        override suspend fun pin(pet: FeaturedPet): Boolean { calls += "pin:${pet.id}"; return true }
        override suspend fun unpin(petId: String) { calls += "unpin:$petId" }
        override suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>) {
            calls += "refresh:${authoritative.keys.sorted().joinToString(",")}"
        }
        override suspend fun wipe() { calls += "wipe" }
        override suspend fun onUserActive(userId: String): UserChangeResult {
            calls += "onUserActive:$userId"
            return nextResult
        }
    }

    /** Like RecordingRepo but returns a scripted sequence of onUserActive results. */
    private class ScriptedRepo(results: List<UserChangeResult>) : RecordingRepo() {
        private val queue = ArrayDeque(results)
        override suspend fun onUserActive(userId: String): UserChangeResult {
            calls += "onUserActive:$userId"
            return queue.removeFirst()
        }
    }
}
