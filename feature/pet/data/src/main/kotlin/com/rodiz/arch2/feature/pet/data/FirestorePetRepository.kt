package com.rodiz.arch2.feature.pet.data

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.PhotoId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestorePetRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PetRepository {

    private val petsCol get() = firestore.collection("pets")

    override fun observeMyPets(): Flow<List<Pet>> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = petsCol
            .whereEqualTo("ownerId", uid)
            .whereIn("state", listOf(PetState.ACTIVE.name, PetState.ARCHIVED.name))
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val pets = snap?.documents.orEmpty().mapNotNull { it.toPetOrNull() }
                trySend(pets)
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override fun observeAllActivePets(limit: Long): Flow<List<Pet>> = callbackFlow {
        val registration = petsCol
            .whereEqualTo("state", PetState.ACTIVE.name)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().mapNotNull { it.toPetOrNull() })
            }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun currentOwnerHasActivePet(): Boolean = withContext(io) {
        val uid = sessionRepo.current()?.userId ?: return@withContext false
        petsCol
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("state", PetState.ACTIVE.name)
            .limit(1)
            .get().await()
            .isEmpty.not()
    }

    override fun observePet(id: PetId): Flow<Pet?> = callbackFlow {
        val registration = petsCol.document(id.value).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.toPetOrNull())
        }
        awaitClose { registration.remove() }
    }.flowOn(io)

    override suspend fun addPet(draft: PetDraft): Pet = withContext(io) {
        val uid = currentUid()
        val docRef = petsCol.document()
        val petId = docRef.id
        val uploadedPhotos = try {
            draft.photos.map { uploadPhotoOrPassThrough(petId, it) }
        } catch (t: Throwable) {
            cleanupStorageBestEffort(petId)
            throw t
        }
        val now = Clock.System.now()
        val finalDraft = draft.copy(photos = uploadedPhotos)
        docRef.set(buildPetMap(uid, finalDraft, PetState.ACTIVE, now, now, null)).await()
        petFromWrite(
            petId = PetId(petId),
            ownerId = uid,
            draft = finalDraft,
            state = PetState.ACTIVE,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
    }

    override suspend fun updatePet(id: PetId, draft: PetDraft): Pet = withContext(io) {
        val uid = currentUid()
        val docRef = petsCol.document(id.value)
        val existing = docRef.get().await().toPetOrNull() ?: error("Pet ${id.value} not found")
        check(existing.ownerId == uid) { "Cannot edit a pet you don't own" }

        val keptPaths = draft.photos
            .mapNotNull { (it.source as? PhotoSource.Remote)?.storagePath }
            .toSet()
        existing.photos
            .mapNotNull { (it.source as? PhotoSource.Remote)?.storagePath }
            .filterNot { it in keptPaths }
            .forEach { path ->
                runCatching { storage.reference.child(path).delete().await() }
            }

        val uploadedPhotos = draft.photos.map { uploadPhotoOrPassThrough(id.value, it) }
        val now = Clock.System.now()
        val finalDraft = draft.copy(photos = uploadedPhotos)
        docRef.set(
            buildPetMap(
                ownerId = uid,
                draft = finalDraft,
                state = existing.state,
                createdAt = existing.createdAt,
                updatedAt = now,
                deletedAt = existing.deletedAt,
                enabled = existing.enabled,
            ),
        ).await()
        petFromWrite(
            petId = id,
            ownerId = uid,
            draft = finalDraft,
            state = existing.state,
            createdAt = existing.createdAt,
            updatedAt = now,
            deletedAt = existing.deletedAt,
            enabled = existing.enabled,
        )
    }

    override suspend fun archivePet(id: PetId) {
        withContext(io) {
            val now = Clock.System.now()
            petsCol.document(id.value).update(
                mapOf(
                    "state" to PetState.ARCHIVED.name,
                    "deletedAt" to now.toTimestamp(),
                    "updatedAt" to now.toTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun restorePet(id: PetId) {
        withContext(io) {
            val now = Clock.System.now()
            petsCol.document(id.value).update(
                mapOf(
                    "state" to PetState.ACTIVE.name,
                    "deletedAt" to FieldValue.delete(),
                    "updatedAt" to now.toTimestamp(),
                ),
            ).await()
        }
    }

    override suspend fun setPetEnabled(id: PetId, enabled: Boolean) {
        withContext(io) {
            val now = Clock.System.now()
            petsCol.document(id.value).update(
                mapOf(
                    "enabled" to enabled,
                    "updatedAt" to now.toTimestamp(),
                ),
            ).await()
        }
    }

    private suspend fun currentUid(): String =
        sessionRepo.current()?.userId ?: error("No signed-in user")

    private suspend fun uploadPhotoOrPassThrough(petId: String, photo: PetPhoto): PetPhoto =
        when (val src = photo.source) {
            is PhotoSource.Remote -> photo
            is PhotoSource.Local -> {
                val photoId = photo.id.value.ifBlank { UUID.randomUUID().toString() }
                val path = "pets/$petId/photos/$photoId.jpg"
                val ref = storage.reference.child(path)
                ref.putFile(Uri.parse(src.uri)).await()
                val url = ref.downloadUrl.await().toString()
                photo.copy(
                    id = PhotoId(photoId),
                    source = PhotoSource.Remote(storagePath = path, downloadUrl = url),
                )
            }
        }

    private suspend fun cleanupStorageBestEffort(petId: String) {
        runCatching {
            storage.reference.child("pets/$petId/photos").listAll().await()
        }.getOrNull()?.items?.forEach { item ->
            runCatching { item.delete().await() }
        }
    }
}
