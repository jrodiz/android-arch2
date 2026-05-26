package com.rodiz.arch2.feature.likes.domain.model

import com.rodiz.arch2.feature.deck.domain.model.DistanceBucket
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import kotlinx.datetime.Instant

/** Like document key: "${fromOwnerId}_${toPetId}" — matches the likes collection's deterministic id. */
@JvmInline
value class LikeKey(val value: String)

/** A like targeting one of my pets, ready for the Likes-you grid. */
data class IncomingLike(
    val key: LikeKey,
    val fromOwnerId: String,
    val toPetId: PetId,
    val anchorPet: Pet, // a representative pet of the liker (their most-recent active pet)
    val likedAt: Instant,
    /**
     * Distance bucket of the liker's owner location from mine, computed in the
     * data layer. Null when either side hasn't set a location (a real fact the
     * UI should fall back gracefully on, not an error).
     */
    val distanceBucket: DistanceBucket? = null,
)
