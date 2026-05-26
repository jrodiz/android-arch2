package com.rodiz.arch2.core.featuredpets.domain

import kotlinx.coroutines.flow.Flow

/**
 * Persistent, locally-cached "pin up to 3 pets to show on the Login screen"
 * store. The cache is intentionally accessible without authentication so the
 * Login hero can render before a user signs in — Firestore rules block
 * unauthenticated reads, so the data must be local. Both the My Pets pin
 * overlay and the Settings → Featured screen drive this repo; they share
 * state with no draft/commit step.
 */
interface FeaturedPetsRepository {
    /** Live featured pets. Emits [FeaturedPetsState] with an empty list until hydrated. */
    fun observe(): Flow<FeaturedPetsState>

    /** Snapshot read for non-Flow callers (e.g. the session-change gate at startup). */
    suspend fun current(): FeaturedPetsState

    /**
     * Add a pet to the featured set. Returns `false` (and is a no-op) when
     * the set is already at [MAX_FEATURED_PETS]; the UI surfaces a snackbar
     * in that case instead of silently demoting an older entry.
     */
    suspend fun pin(pet: FeaturedPet): Boolean

    /** Remove a pet from the featured set. No-op when the id isn't pinned. */
    suspend fun unpin(petId: String)

    /**
     * Reconcile cached metadata against the authoritative live pet list
     * (typically `ObserveMyPetsUseCase` output once signed in). Drops cache
     * entries whose id is no longer present, refreshes name/species/avatarUrl
     * for ids that are present, preserves order for the rest.
     */
    suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>)

    /**
     * Clears the featured set. Called only by [FeaturedPetsSessionGate] when
     * a different user signs in — sign-out itself preserves the cache.
     * (`FeaturedPetsSessionGate` lives in `:core:featuredpets:data`.)
     */
    suspend fun wipe()

    /**
     * Records the currently-signed-in uid in the cache and returns whether
     * it differs from the previously-recorded one. Implementations update
     * the stored uid atomically so the caller can act on the result without
     * a race window.
     */
    suspend fun onUserActive(userId: String): UserChangeResult
}

enum class UserChangeResult {
    /** No uid was previously recorded; this is the first sign-in on this install. */
    FIRST_USER,

    /** The recorded uid matches the active uid; cache is theirs already. */
    SAME_USER,

    /** A different uid is active now; caller should wipe the cache. */
    USER_CHANGED,
}
