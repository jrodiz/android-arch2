package com.rodiz.arch2.core.petlookup.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.petlookup.domain.PetDisplay
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestorePetLookupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PetLookupRepository {

    private val petsCol get() = firestore.collection("pets")

    override fun observeAll(): Flow<Map<String, PetDisplay>> = callbackFlow {
        val registration = petsCol.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val map = snap?.documents.orEmpty()
                .mapNotNull { it.toPetDisplay() }
                .associateBy { it.id }
            trySend(map)
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override fun observe(petId: String): Flow<PetDisplay?> = callbackFlow {
        val registration = petsCol.document(petId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toPetDisplay())
        }
        awaitClose { registration.remove() }
    }.flowOn(io)
}

private fun DocumentSnapshot.toPetDisplay(): PetDisplay? {
    if (!exists()) return null
    val ownerId = getString("ownerId") ?: return null
    val name = getString("name") ?: return null
    val species = getString("species")
    // The pet doc stores its photos under a `photos` array of objects with `url` keys;
    // surface the first one as a simple avatar URL so cross-feature display surfaces
    // can render a quick thumbnail without parsing the full photo schema.
    val photos = get("photos") as? List<*>
    val avatarUrl = photos?.firstNotNullOfOrNull { (it as? Map<*, *>)?.get("url") as? String }
    val intents = (get("intents") as? List<*>)
        ?.mapNotNullTo(mutableSetOf()) { it as? String }
        ?: emptySet()
    return PetDisplay(
        id = id,
        ownerId = ownerId,
        name = name,
        species = species,
        avatarUrl = avatarUrl,
        intents = intents,
    )
}
