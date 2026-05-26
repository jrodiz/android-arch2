package com.rodiz.arch2.core.featuredpets.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreFeaturedPetsRepositoryTest {

    @field:TempDir
    lateinit var tempDir: File

    @Test
    fun `pin adds to ordered list and returns true`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            assertTrue(repo.pin(FeaturedPet("p1", "Rex")))
            assertTrue(repo.pin(FeaturedPet("p2", "Mochi")))
            val state = repo.current()
            assertEquals(listOf("p1", "p2"), state.featured.map { it.id })
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `pin at max returns false without mutating`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            assertTrue(repo.pin(FeaturedPet("p1", "Rex")))
            assertTrue(repo.pin(FeaturedPet("p2", "Mochi")))
            assertTrue(repo.pin(FeaturedPet("p3", "Bun")))
            assertFalse(repo.pin(FeaturedPet("p4", "Extra")))
            val state = repo.current()
            assertEquals(listOf("p1", "p2", "p3"), state.featured.map { it.id })
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `pin same id twice is idempotent and returns true`() = runTest {
        // Defensive: double-tap on the pin icon shouldn't surface a misleading
        // "limit reached" error or shift order.
        val (repo, dsScope, _) = newRepo()
        try {
            assertTrue(repo.pin(FeaturedPet("p1", "Rex")))
            assertTrue(repo.pin(FeaturedPet("p1", "Rex-updated")))
            val state = repo.current()
            assertEquals(listOf("p1"), state.featured.map { it.id })
            // First pin wins; we don't try to "update" on re-pin (use refreshFrom for that).
            assertEquals("Rex", state.featured.single().name)
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `unpin removes by id and is a no-op when absent`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            repo.pin(FeaturedPet("p1", "Rex"))
            repo.pin(FeaturedPet("p2", "Mochi"))
            repo.unpin("p1")
            assertEquals(listOf("p2"), repo.current().featured.map { it.id })
            repo.unpin("nonexistent")
            assertEquals(listOf("p2"), repo.current().featured.map { it.id })
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `refreshFrom drops absent ids and refreshes present ones`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            repo.pin(FeaturedPet("p1", "Rex"))
            repo.pin(FeaturedPet("p2", "Mochi"))
            repo.pin(FeaturedPet("p3", "Bun"))
            // Authoritative says p2 was renamed and p3 was deleted; p1 unchanged.
            repo.refreshFrom(
                mapOf(
                    "p1" to FeaturedPet("p1", "Rex", avatarUrl = "https://x/rex.jpg"),
                    "p2" to FeaturedPet("p2", "Mochi II"),
                ),
            )
            val featured = repo.current().featured
            assertEquals(listOf("p1", "p2"), featured.map { it.id })
            assertEquals("https://x/rex.jpg", featured.first { it.id == "p1" }.avatarUrl)
            assertEquals("Mochi II", featured.first { it.id == "p2" }.name)
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `observe emits initial empty then mutations`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            repo.observe().test {
                assertTrue(awaitItem().featured.isEmpty())
                repo.pin(FeaturedPet("p1", "Rex"))
                assertEquals(listOf("p1"), awaitItem().featured.map { it.id })
                repo.unpin("p1")
                assertTrue(awaitItem().featured.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `wipe clears featured set but retains last user id`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            assertEquals(UserChangeResult.FIRST_USER, repo.onUserActive("uidA"))
            repo.pin(FeaturedPet("p1", "Rex"))
            repo.wipe()
            assertTrue(repo.current().featured.isEmpty())
            // A same-user re-activation after wipe reports SAME_USER (not FIRST_USER),
            // proving the last-user-id key survived the wipe.
            assertEquals(UserChangeResult.SAME_USER, repo.onUserActive("uidA"))
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `onUserActive transitions across users`() = runTest {
        val (repo, dsScope, _) = newRepo()
        try {
            assertEquals(UserChangeResult.FIRST_USER, repo.onUserActive("uidA"))
            assertEquals(UserChangeResult.SAME_USER, repo.onUserActive("uidA"))
            assertEquals(UserChangeResult.USER_CHANGED, repo.onUserActive("uidB"))
            assertEquals(UserChangeResult.SAME_USER, repo.onUserActive("uidB"))
            assertEquals(UserChangeResult.USER_CHANGED, repo.onUserActive("uidA"))
        } finally {
            dsScope.cancel()
        }
    }

    @Test
    fun `corrupt JSON reads as empty list and does not crash`() = runTest {
        val (repo, dsScope, dataStore) = newRepo()
        try {
            // Write garbage to the JSON key directly.
            dataStore.edit { it[stringPreferencesKey("featured_pets_json")] = "{not valid json" }
            val state = repo.current()
            assertTrue(state.featured.isEmpty())
            // And we can still pin successfully on top of the bad state.
            assertTrue(repo.pin(FeaturedPet("p1", "Rex")))
            assertEquals(listOf("p1"), repo.current().featured.map { it.id })
        } finally {
            dsScope.cancel()
        }
    }

    // --- helpers ---

    private data class Harness(
        val repo: DataStoreFeaturedPetsRepository,
        val scope: CoroutineScope,
        val dataStore: DataStore<Preferences>,
    )

    private fun newRepo(): Harness {
        // Each test owns its own DataStore file + scope so they don't leak
        // state between cases (DataStore caches per-file, so reusing a path
        // across runTest invocations cross-pollinates).
        val scope = CoroutineScope(Job() + UnconfinedTestDispatcher())
        val unique = "featured_${System.nanoTime()}.preferences_pb"
        val ds = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempDir, unique) },
        )
        return Harness(
            repo = DataStoreFeaturedPetsRepository(ds),
            scope = scope,
            dataStore = ds,
        )
    }
}
