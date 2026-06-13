package com.rodiz.arch2.feature.deck.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.geo.Haversine
import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.DistanceBucket
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

@Singleton
internal class FirestoreDeckRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val petRepo: PetRepository,
    private val ownerLookup: OwnerLookupRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DeckRepository {

    private val likesCol get() = firestore.collection("likes")
    private val passesCol get() = firestore.collection("passes")
    private val matchesCol get() = firestore.collection("matches")
    private val ownersCol get() = firestore.collection("owners")
    private val blocksCol get() = firestore.collection("blocks")

    private data class LastSwipe(val petId: PetId, val action: SwipeAction, val at: Instant)
    private var lastSwipe: LastSwipe? = null

    /** Pets swiped this session — optimistic UI filter; the persistent check happens server-side. */
    private val sessionSwiped = mutableSetOf<String>()

    override fun observeDeck(filters: FilterPrefs): Flow<DeckSnapshot> = flow {
        val uid = sessionRepo.current()?.userId
            ?: run { emit(DeckSnapshot(emptyList(), DeckState.LOADING)); return@flow }

        var loggedOnce = false
        combine(
            petRepo.observeAllActivePets(),
            observePausedOwnerIds(),
            observeBlockHideSet(uid),
            observeOwnerLocations(),
            // Pair owner display with the set of pets I've already liked/passed — keeps the
            // outer combine at 5 typed sources.
            combine(ownerLookup.observeAll(), observeMyActedPetIds(uid)) { owners, acted ->
                owners to acted
            },
        ) { pets, paused, blocked, locations, ownersAndActed ->
            val (owners, acted) = ownersAndActed
            val hideOwners = paused + blocked
            val myLoc = locations[uid]
            val maxKm = filters.maxDistanceKm.toDouble()
            if (!loggedOnce) {
                loggedOnce = true
                Log.d(
                    "TinPet.Deck",
                    "observeDeck uid=$uid pets=${pets.size} paused=${paused.size} " +
                        "blocked=${blocked.size} acted=${acted.size} " +
                        "locations=${locations.size} owners=${owners.size} maxKm=$maxKm",
                )
            }
            val cards = pets
                .filter { it.ownerId != uid }
                .filter { it.ownerId !in hideOwners }
                .filter { it.enabled }
                .filter { it.id.value !in sessionSwiped }
                .filter { it.id.value !in acted }
                .filter { it.species in filters.species }
                .filter { it.intents.any { intent -> intent in filters.intents } }
                .filter { pet -> withinDistance(myLoc, locations[pet.ownerId], maxKm) }
                .map { pet ->
                    val theirLoc = locations[pet.ownerId]
                    val bucket = if (myLoc != null && theirLoc != null) {
                        val km = Haversine.distanceKm(
                            myLoc.latitude, myLoc.longitude,
                            theirLoc.latitude, theirLoc.longitude,
                        )
                        DistanceBucket.fromKm(km)
                    } else {
                        null
                    }
                    DeckCard(pet = pet, owner = owners[pet.ownerId], distanceBucket = bucket)
                }
            val state = if (cards.isEmpty()) DeckState.EXHAUSTED else DeckState.READY
            DeckSnapshot(cards, state)
        }.collect { emit(it) }
    }.flowOn(io)

    // Distance gate. If we don't know my location yet we don't filter — better to
    // show pets than to leave the deck empty during first-time setup. If we know
    // mine but theirs is missing, we exclude them: otherwise distance-based matching
    // means nothing.
    private fun withinDistance(
        mine: FirestoreGeoPoint?,
        theirs: FirestoreGeoPoint?,
        maxKm: Double,
    ): Boolean {
        if (mine == null) return true
        if (theirs == null) return false
        val km = Haversine.distanceKm(mine.latitude, mine.longitude, theirs.latitude, theirs.longitude)
        return km <= maxKm
    }

    // Live map of every owner who has set a location → their Firestore GeoPoint.
    // Uses orderBy("geohash") to implicitly skip owners that haven't set one.
    private fun observeOwnerLocations(): Flow<Map<String, FirestoreGeoPoint>> = callbackFlow {
        val registration = ownersCol
            .orderBy("geohash", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val map = snap?.documents.orEmpty().mapNotNull { doc ->
                    val loc = doc.get("location") as? FirestoreGeoPoint ?: return@mapNotNull null
                    doc.id to loc
                }.toMap()
                trySend(map)
            }
        awaitClose { registration.remove() }
    }

    // owners.paused == true → hide all their pets from every other deck.
    private fun observePausedOwnerIds(): Flow<Set<String>> = callbackFlow {
        val registration = ownersCol
            .whereEqualTo("paused", true)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().map { it.id }.toSet())
            }
        awaitClose { registration.remove() }
    }

    // Two-sided block filter: owners I've blocked AND owners who've blocked me.
    // Either direction hides the other party's pets from my deck.
    private fun observeBlockHideSet(uid: String): Flow<Set<String>> = combine(
        observeBlockedOtherIds(field = "ownerId", uid = uid, targetField = "blockedOwnerId"),
        observeBlockedOtherIds(field = "blockedOwnerId", uid = uid, targetField = "ownerId"),
    ) { iBlocked, blockedMe -> iBlocked + blockedMe }

    private fun observeBlockedOtherIds(
        field: String,
        uid: String,
        targetField: String,
    ): Flow<Set<String>> = callbackFlow {
        val registration = blocksCol
            .whereEqualTo(field, uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val ids = snap?.documents.orEmpty()
                    .mapNotNull { it.getString(targetField) }
                    .toSet()
                trySend(ids)
            }
        awaitClose { registration.remove() }
    }

    // Pets I've already liked or passed (server-side) — excluded from my deck so they
    // don't reappear across sessions (the in-memory `sessionSwiped` only covers the
    // current process). Passes are reversible via "Review who you passed", which deletes
    // the pass docs and lets the snapshot re-include the pet on the next emission.
    private fun observeMyActedPetIds(uid: String): Flow<Set<String>> = combine(
        observeActedToPetIds(likesCol.whereEqualTo("fromOwnerId", uid)),
        observeActedToPetIds(passesCol.whereEqualTo("ownerId", uid)),
    ) { liked, passed -> liked + passed }

    private fun observeActedToPetIds(query: Query): Flow<Set<String>> = callbackFlow {
        val registration = query.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.documents.orEmpty().mapNotNull { it.getString("toPetId") }.toSet())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult = withContext(io) {
        val me = sessionRepo.current()?.userId ?: error("No signed-in user")
        val key = "${me}_${petId.value}"
        val now = Clock.System.now()
        sessionSwiped.add(petId.value)

        if (action == SwipeAction.PASS) {
            passesCol.document(key).set(
                mapOf(
                    "ownerId" to me,
                    "toPetId" to petId.value,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            lastSwipe = LastSwipe(petId, action, now)
            return@withContext SwipeResult.Pending
        }

        // LIKE — gate on having at least one published pet (done at swipe time, not deck open).
        if (!petRepo.currentOwnerHasActivePet()) {
            sessionSwiped.remove(petId.value)
            return@withContext SwipeResult.RequiresPet
        }

        val targetSnap = firestore.collection("pets").document(petId.value).get().await()
        val targetOwner = targetSnap.getString("ownerId") ?: error("target pet missing ownerId")

        // Client only writes the like. The onLikeCreate Cloud Function handles reciprocity +
        // creates the match doc atomically. We then poll briefly for the match doc to surface
        // the celebration overlay on this device — if the Function takes longer than the poll
        // window, the user still gets the match push notification.
        likesCol.document(key).set(
            mapOf(
                "fromOwnerId" to me,
                "toOwnerId" to targetOwner,
                "toPetId" to petId.value,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        lastSwipe = LastSwipe(petId, action, now)

        val matchId = listOf(me, targetOwner).sorted().joinToString("_")
        val matchDocRef = matchesCol.document(matchId)

        // Already a match (e.g., re-swipe after rewind)? Surface immediately.
        if (matchDocRef.get().await().exists()) return@withContext SwipeResult.Match(matchId)

        // Otherwise wait up to ~2 seconds for the Function to create the match.
        val matched = pollForMatch(matchId, attempts = 8, intervalMs = 250)
        if (matched) SwipeResult.Match(matchId) else SwipeResult.Pending
    }

    private suspend fun pollForMatch(matchId: String, attempts: Int, intervalMs: Long): Boolean {
        val ref = matchesCol.document(matchId)
        repeat(attempts) {
            delay(intervalMs)
            if (ref.get().await().exists()) return true
        }
        return false
    }

    override suspend fun undoLastSwipe(): Pet? = withContext(io) {
        val last = lastSwipe ?: return@withContext null
        val ageSeconds = (Clock.System.now() - last.at).inWholeSeconds
        if (ageSeconds > 60) return@withContext null
        val me = sessionRepo.current()?.userId ?: return@withContext null
        val key = "${me}_${last.petId.value}"
        when (last.action) {
            SwipeAction.LIKE -> likesCol.document(key).delete().await()
            SwipeAction.PASS -> passesCol.document(key).delete().await()
        }
        sessionSwiped.remove(last.petId.value)
        lastSwipe = null
        // Return the un-swiped pet so the ViewModel can re-prepend it on top of the
        // deck immediately. We can't rely on the deck snapshot to re-include it:
        // `observeAllActivePets` reacts to pet changes, not to pass/like deletions,
        // so deleting the swipe doc above produces no new emission (see `swipe()`).
        petRepo.observePet(last.petId).first()
    }

    override suspend fun clearTodayPasses(): Int = withContext(io) {
        val me = sessionRepo.current()?.userId ?: return@withContext 0
        // Local-midnight "today" — matches the wall-clock day the user sees, not UTC.
        val tz = TimeZone.currentSystemDefault()
        val startOfDayMillis = Clock.System.now()
            .toLocalDateTime(tz)
            .date
            .atStartOfDayIn(tz)
            .toEpochMilliseconds()
        val snap = passesCol
            .whereEqualTo("ownerId", me)
            .whereGreaterThanOrEqualTo("createdAt", Timestamp(startOfDayMillis / 1000, 0))
            .get()
            .await()
        if (snap.isEmpty) return@withContext 0
        val batch = firestore.batch()
        snap.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        // Drop the in-memory session filter for the deleted pets so the deck snapshot
        // re-includes them on the next emission.
        snap.documents.forEach { d ->
            d.getString("toPetId")?.let { sessionSwiped.remove(it) }
        }
        snap.size()
    }
}
