package com.rodiz.arch2.feature.likes.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.geo.Haversine
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.deck.domain.model.DistanceBucket
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

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
    private val ownersCol get() = firestore.collection("owners")
    private val matchesCol get() = firestore.collection("matches")

    // Re-subscribe on every session change so an in-app account switch (sign out → sign in a
    // different user, no restart) re-keys to the new user's incoming likes instead of leaking
    // the previous user's. [D-010]
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLikesYou(): Flow<List<IncomingLike>> =
        sessionRepo.observe().flatMapLatest { session ->
            val uid = session?.userId
            if (uid == null) flowOf(emptyList()) else likesForUid(uid)
        }

    private fun likesForUid(uid: String): Flow<List<IncomingLike>> = callbackFlow {
        // Two inputs drive this list: the incoming-like docs, and the owners I've matched
        // with (whose like must drop out once we match — otherwise it lingers in both the
        // Likes-you grid and the badge, and the empty state is unreachable). A match forms
        // on the *reciprocal* like, which never touches my toOwnerId==uid query, so the
        // likes listener alone can't see it — we watch matches too and recompute on either
        // change. [D-006]
        val latestDocs = AtomicReference<List<DocumentSnapshot>>(emptyList())
        val matchedOwners = AtomicReference<Set<String>>(emptySet())
        val likesArrived = AtomicReference(false)
        // Monotonic token so a slower, older resolve can't overwrite a newer emission.
        val version = AtomicLong(0)

        fun recompute() {
            // Wait for the first real likes snapshot before emitting, so a matches-first
            // callback can't push a spurious empty list ahead of the actual data.
            if (!likesArrived.get()) return
            val token = version.incrementAndGet()
            launch {
                val docs = latestDocs.get()
                val matched = matchedOwners.get()
                val passedSet = loadPassedSet(uid)
                val myLoc = loadOwnerLocation(uid)
                val raws = docs.mapNotNull { it.toRawLikeOrNull() }
                    .filterNot { passedKey(uid, it) in passedSet }
                    .filterNot { it.fromOwnerId in matched }
                val resolved = raws.mapNotNull { raw ->
                    val anchor = resolveAnchorPet(raw.fromOwnerId) ?: return@mapNotNull null
                    val theirLoc = loadOwnerLocation(raw.fromOwnerId)
                    val bucket = bucketBetween(myLoc, theirLoc)
                    IncomingLike(
                        key = LikeKey(raw.key),
                        fromOwnerId = raw.fromOwnerId,
                        toPetId = PetId(raw.toPetId),
                        anchorPet = anchor,
                        likedAt = raw.createdAt,
                        distanceBucket = bucket,
                    )
                }
                // Drop this result if a newer recompute has started while we resolved.
                if (token == version.get()) send(resolved)
            }
        }

        val likesReg = likesCol
            .whereEqualTo("toOwnerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                latestDocs.set(snap?.documents.orEmpty())
                likesArrived.set(true)
                recompute()
            }
        // Best-effort: a matches-listener error keeps the last-known matched set rather than
        // tearing down the whole likes flow.
        val matchesReg = matchesCol
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                matchedOwners.set(
                    snap?.documents.orEmpty()
                        .flatMap { (it.get("participants") as? List<*>).orEmpty() }
                        .filterIsInstance<String>()
                        .filterNot { it == uid }
                        .toSet(),
                )
                recompute()
            }
        awaitClose {
            likesReg.remove()
            matchesReg.remove()
        }
    }.flowOn(io)

    private suspend fun loadPassedSet(uid: String): Set<String> =
        passedLikesCol.whereEqualTo("toOwnerId", uid).get().await()
            .documents.map { it.id }.toSet()

    private fun passedKey(uid: String, raw: RawLike): String =
        "${uid}_${raw.fromOwnerId}_${raw.toPetId}"

    /**
     * Reads an owner's stored GeoPoint, or null when the doc / location is absent.
     * Best-effort: any Firestore failure resolves to null so a transient network
     * error doesn't block the whole likes flow from emitting.
     */
    private suspend fun loadOwnerLocation(ownerId: String): FirestoreGeoPoint? = runCatching {
        ownersCol.document(ownerId).get().await().get("location") as? FirestoreGeoPoint
    }.getOrNull()

    private fun bucketBetween(mine: FirestoreGeoPoint?, theirs: FirestoreGeoPoint?): DistanceBucket? {
        if (mine == null || theirs == null) return null
        val km = Haversine.distanceKm(mine.latitude, mine.longitude, theirs.latitude, theirs.longitude)
        return DistanceBucket.fromKm(km)
    }

    /** Picks the liker's most-recently-updated ACTIVE pet. Returns null if they have none. */
    private suspend fun resolveAnchorPet(fromOwnerId: String): Pet? = runCatching {
        val snap = petsCol
            .whereEqualTo("ownerId", fromOwnerId)
            .whereEqualTo("state", PetState.ACTIVE.name)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get().await()
        snap.documents.firstOrNull()?.toPetOrNull()
    }.getOrNull()

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
            ?: return@withContext SwipeResult.Pending // liker has no pet to like back on
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
            breed = getString("breed"),
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
