package com.rodiz.arch2.feature.likes.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import com.rodiz.arch2.feature.likes.domain.repository.LikesYouRepository
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.PhotoId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.model.Species
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreLikesYouRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val deckRepo: DeckRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LikesYouRepository {

    private val likesCol get() = firestore.collection("likes")
    private val passedLikesCol get() = firestore.collection("passedLikes")
    private val petsCol get() = firestore.collection("pets")

    override fun observeLikesYou(): Flow<List<IncomingLike>> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = likesCol
            .whereEqualTo("toOwnerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val docs = snap?.documents.orEmpty()
                if (docs.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Resolve passed-set + anchor pets async; using callbackFlow's producer scope so
                // we don't block the snapshot callback thread (the listener runs on main by default).
                launch {
                    val passedSet = loadPassedSet(uid)
                    val raws = docs.mapNotNull { it.toRawLikeOrNull() }
                        .filterNot { passedKey(uid, it) in passedSet }
                    val resolved = raws.mapNotNull { raw ->
                        val anchor = resolveAnchorPet(raw.fromOwnerId) ?: return@mapNotNull null
                        IncomingLike(
                            key = LikeKey(raw.key),
                            fromOwnerId = raw.fromOwnerId,
                            toPetId = PetId(raw.toPetId),
                            anchorPet = anchor,
                            likedAt = raw.createdAt,
                        )
                    }
                    send(resolved)
                }
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    private suspend fun loadPassedSet(uid: String): Set<String> =
        passedLikesCol.whereEqualTo("toOwnerId", uid).get().await()
            .documents.map { it.id }.toSet()

    private fun passedKey(uid: String, raw: RawLike): String =
        "${uid}_${raw.fromOwnerId}_${raw.toPetId}"

    /** Picks the liker's most-recently-updated ACTIVE pet. Returns null if they have none. */
    private suspend fun resolveAnchorPet(fromOwnerId: String): Pet? {
        return runCatching {
            val snap = petsCol
                .whereEqualTo("ownerId", fromOwnerId)
                .whereEqualTo("state", PetState.ACTIVE.name)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            snap.documents.firstOrNull()?.toPetOrNull()
        }.getOrNull()
    }

    override suspend fun pass(key: LikeKey) {
        withContext(io) {
            val me = sessionRepo.current()?.userId ?: return@withContext
            // key.value is "${fromOwnerId}_${toPetId}". passId is "${me}_{key}".
            val (from, to) = key.value.split("_", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val passId = "${me}_${from}_$to"
            passedLikesCol.document(passId).set(
                mapOf(
                    "toOwnerId" to me,
                    "fromOwnerId" to from,
                    "toPetId" to to,
                    "passedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun likeBack(key: LikeKey): SwipeResult = withContext(io) {
        // Find the anchor pet to like back on (deterministically; same rule the grid used).
        val parts = key.value.split("_", limit = 2)
        val fromOwnerId = parts[0]
        val anchor = resolveAnchorPet(fromOwnerId)
            ?: return@withContext SwipeResult.Pending  // liker has no pet to like back on
        deckRepo.submitSwipe(anchor.id, SwipeAction.LIKE)
    }
}

private data class RawLike(
    val key: String,
    val fromOwnerId: String,
    val toOwnerId: String,
    val toPetId: String,
    val createdAt: Instant,
)

private fun DocumentSnapshot.toRawLikeOrNull(): RawLike? {
    if (!exists()) return null
    return try {
        val from = getString("fromOwnerId") ?: return null
        val to = getString("toOwnerId") ?: return null
        val petId = getString("toPetId") ?: return null
        val createdAt = getTimestamp("createdAt")?.toKxInstant() ?: return null
        RawLike(id, from, to, petId, createdAt)
    } catch (_: Throwable) {
        null
    }
}

// Duplicates the pet snapshot reader from :feature:pet:data because the scaffold rules forbid
// :feature:likes:data → :feature:pet:data. Same shape; if these drift, move to a shared core
// module later.
@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toPetOrNull(): Pet? {
    if (!exists()) return null
    return try {
        val ownerId = getString("ownerId") ?: return null
        val name = getString("name") ?: return null
        val ageYears = getLong("ageYears")?.toInt() ?: 0
        val ageIsApproximate = getBoolean("ageIsApproximate") ?: false
        val speciesName = getString("species") ?: return null
        val species = Species.entries.firstOrNull { it.name == speciesName } ?: return null
        val intents = (get("intents") as? List<String>).orEmpty()
            .mapNotNull { n -> Intent.entries.firstOrNull { it.name == n } }
            .toSet()
        val photos = (get("photos") as? List<Map<String, Any?>>).orEmpty()
            .mapNotNull { entry ->
                val pid = entry["id"] as? String ?: return@mapNotNull null
                val path = entry["storagePath"] as? String ?: return@mapNotNull null
                val url = entry["downloadUrl"] as? String ?: return@mapNotNull null
                PetPhoto(PhotoId(pid), PhotoSource.Remote(path, url))
            }
        val state = PetState.entries.firstOrNull { it.name == (getString("state") ?: "ACTIVE") }
            ?: PetState.ACTIVE
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
            bio = getString("bio"),
            state = state,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
    } catch (_: Throwable) {
        null
    }
}

private fun Timestamp.toKxInstant(): Instant =
    Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
