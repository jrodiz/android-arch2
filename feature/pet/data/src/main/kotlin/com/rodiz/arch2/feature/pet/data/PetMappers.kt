package com.rodiz.arch2.feature.pet.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.PhotoId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.model.Species
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

/** Builds the Firestore document data for a pet record. */
internal fun buildPetMap(
    ownerId: String,
    draft: PetDraft,
    state: PetState,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    enabled: Boolean = true,
): Map<String, Any?> = mapOf(
    "ownerId" to ownerId,
    "name" to draft.name,
    "ageYears" to draft.ageYears.toLong(),
    "ageIsApproximate" to draft.ageIsApproximate,
    "species" to draft.species?.name,
    "speciesCategory" to draft.species?.category?.name,
    "intents" to draft.intents.map { it.name },
    "photos" to draft.photos.map { photo ->
        val remote = photo.source as? PhotoSource.Remote
            ?: error("Photo ${photo.id.value} is not uploaded before write")
        mapOf(
            "id" to photo.id.value,
            "storagePath" to remote.storagePath,
            "downloadUrl" to remote.downloadUrl,
        )
    },
    "bio" to draft.bio,
    "state" to state.name,
    "enabled" to enabled,
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
    "deletedAt" to deletedAt?.toTimestamp(),
)

/** Reads a Pet from a Firestore /pets/{petId} document. Returns null if any required field is missing/malformed. */
@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toPetOrNull(): Pet? {
    if (!exists()) return null
    val id = id
    return try {
        val ownerId = getString("ownerId") ?: return null
        val name = getString("name") ?: return null
        val ageYears = getLong("ageYears")?.toInt() ?: 0
        val ageIsApproximate = getBoolean("ageIsApproximate") ?: false
        val speciesName = getString("species") ?: return null
        val species = Species.entries.firstOrNull { it.name == speciesName } ?: return null
        val intents = (get("intents") as? List<String>).orEmpty()
            .mapNotNull { name -> Intent.entries.firstOrNull { it.name == name } }
            .toSet()
        val photos = (get("photos") as? List<Map<String, Any?>>).orEmpty()
            .mapNotNull { entry ->
                val photoId = entry["id"] as? String ?: return@mapNotNull null
                val storagePath = entry["storagePath"] as? String ?: return@mapNotNull null
                val downloadUrl = entry["downloadUrl"] as? String ?: return@mapNotNull null
                PetPhoto(
                    id = PhotoId(photoId),
                    source = PhotoSource.Remote(storagePath = storagePath, downloadUrl = downloadUrl),
                )
            }
        val bio = getString("bio")
        val stateName = getString("state") ?: PetState.ACTIVE.name
        val state = PetState.entries.firstOrNull { it.name == stateName } ?: PetState.ACTIVE
        // Default true so legacy docs written before the toggle existed stay visible by default.
        val enabled = getBoolean("enabled") ?: true
        val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
        val updatedAt = getTimestamp("updatedAt")?.toKxInstant() ?: Instant.fromEpochMilliseconds(0)
        val deletedAt = getTimestamp("deletedAt")?.toKxInstant()
        Pet(
            id = PetId(id),
            ownerId = ownerId,
            name = name,
            ageYears = ageYears,
            ageIsApproximate = ageIsApproximate,
            species = species,
            intents = intents,
            photos = photos,
            bio = bio,
            state = state,
            enabled = enabled,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    } catch (_: Throwable) {
        null
    }
}

/** Constructs a Pet domain object for the just-written record (no DB round-trip). */
internal fun petFromWrite(
    petId: PetId,
    ownerId: String,
    draft: PetDraft,
    state: PetState,
    createdAt: Instant,
    updatedAt: Instant,
    deletedAt: Instant?,
    enabled: Boolean = true,
): Pet = Pet(
    id = petId,
    ownerId = ownerId,
    name = draft.name,
    ageYears = draft.ageYears,
    ageIsApproximate = draft.ageIsApproximate,
    species = requireNotNull(draft.species) { "species must be validated upstream" },
    intents = draft.intents,
    photos = draft.photos,
    bio = draft.bio,
    state = state,
    enabled = enabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun Instant.toTimestamp(): Timestamp = Timestamp(Date.from(toJavaInstant()))

internal fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
