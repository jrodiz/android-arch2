package com.rodiz.arch2.core.ownerlookup.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreOwnerLookupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OwnerLookupRepository {

    private val ownersCol get() = firestore.collection("owners")

    override fun observeAll(): Flow<Map<String, OwnerDisplay>> = callbackFlow {
        val registration = ownersCol.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val map = snap?.documents.orEmpty()
                .mapNotNull { it.toOwnerDisplay() }
                .associateBy { it.id }
            trySend(map)
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override fun observe(ownerId: String): Flow<OwnerDisplay?> = callbackFlow {
        val registration = ownersCol.document(ownerId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toOwnerDisplay())
        }
        awaitClose { registration.remove() }
    }.flowOn(io)
}

private fun DocumentSnapshot.toOwnerDisplay(): OwnerDisplay? {
    if (!exists()) return null
    val firstName = getString("firstName") ?: return null
    val avatarUrl = getString("avatarUrl")
    return OwnerDisplay(id = id, firstName = firstName, avatarUrl = avatarUrl)
}
