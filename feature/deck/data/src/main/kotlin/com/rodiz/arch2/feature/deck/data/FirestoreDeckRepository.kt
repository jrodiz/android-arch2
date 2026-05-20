package com.rodiz.arch2.feature.deck.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.FilterPrefs
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreDeckRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val petRepo: PetRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DeckRepository {

    private val likesCol get() = firestore.collection("likes")
    private val passesCol get() = firestore.collection("passes")
    private val matchesCol get() = firestore.collection("matches")

    private data class LastSwipe(val petId: PetId, val action: SwipeAction, val at: Instant)
    private var lastSwipe: LastSwipe? = null

    /** Pets swiped this session — optimistic UI filter; the persistent check happens server-side. */
    private val sessionSwiped = mutableSetOf<String>()

    override fun observeDeck(filters: FilterPrefs): Flow<DeckSnapshot> = flow {
        val uid = sessionRepo.current()?.userId
            ?: run { emit(DeckSnapshot(emptyList(), DeckState.LOADING)); return@flow }

        var loggedOnce = false
        petRepo.observeAllActivePets()
            .map { pets ->
                // One-shot diagnostic — first emission only, so logcat stays clean.
                // Surfaces a uid/ownerId mismatch (e.g. stale handle pets from before
                // the auth-uid fix) immediately instead of via a console trip.
                if (!loggedOnce) {
                    loggedOnce = true
                    Log.d(
                        "TinPet.Deck",
                        "observeDeck uid=$uid pets=${pets.size} " +
                            "sampleOwnerIds=${pets.take(3).map { it.ownerId }}",
                    )
                }
                val cards = pets
                    .filter { it.ownerId != uid }
                    .filter { it.enabled }
                    .filter { it.id.value !in sessionSwiped }
                    .filter { it.species.category in filters.speciesCategories }
                    .filter { it.intents.any { intent -> intent in filters.intents } }
                    .map { DeckCard(it) }
                val state = if (cards.isEmpty()) DeckState.EXHAUSTED else DeckState.READY
                DeckSnapshot(cards, state)
            }
            .collect { emit(it) }
    }.flowOn(io)

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
        null  // The deck snapshot will re-include the pet on next observation tick.
    }
}
