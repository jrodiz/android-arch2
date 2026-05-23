package com.rodiz.arch2.feature.profile.domain.repository

import com.rodiz.arch2.feature.profile.domain.model.GeoPoint
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import kotlinx.coroutines.flow.Flow

interface OwnerProfileRepository {
    /** Live profile for the currently signed-in owner. Emits null while loading or signed out. */
    fun observeMyProfile(): Flow<OwnerProfile?>

    /** Rename the signed-in owner. 1..30 chars, trimmed. */
    suspend fun updateFirstName(name: String)

    /** Update the signed-in owner's bio. 0..150 chars, trimmed. */
    suspend fun updateBio(bio: String)

    /** Uploads the local image, then persists the resulting download URL on the owner doc. */
    suspend fun updateAvatar(localUri: String)

    /** Hide my pets from other owners' decks. Toggleable from Settings → Privacy. */
    suspend fun setPaused(paused: Boolean)

    /** Set the owner's coarse location (used by deck distance filter, later iterations). */
    suspend fun updateLocation(point: GeoPoint)
}
