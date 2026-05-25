package com.rodiz.arch2.feature.settings.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.settings.domain.model.DataExportStatus
import com.rodiz.arch2.feature.settings.domain.repository.DataExportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirestoreDataExportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DataExportRepository {

    private val col get() = firestore.collection("dataExports")

    override fun observeMyExport(): Flow<DataExportStatus> = flow {
        val uid = sessionRepository.current()?.userId
        if (uid == null) {
            emit(DataExportStatus.Idle)
            return@flow
        }
        emitAll(observeForUid(uid))
    }.flowOn(io)

    private fun observeForUid(uid: String): Flow<DataExportStatus> = callbackFlow {
        val registration = col.document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(DataExportStatus.Failed(err.message))
                return@addSnapshotListener
            }
            trySend(snap.toStatus())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun requestExport() {
        withContext(io) {
            val uid = sessionRepository.current()?.userId ?: error("No signed-in user")
            // Delete any prior request doc first — `onDataExportRequest` is a
            // document-created trigger, so `set()` over an existing doc wouldn't
            // re-fire it. The delete failing (doc missing) is fine.
            runCatching { col.document(uid).delete().await() }
            col.document(uid).set(
                mapOf(
                    "requestedAt" to FieldValue.serverTimestamp(),
                    "status" to "queued",
                ),
            ).await()
        }
    }

    override suspend fun acknowledge() {
        withContext(io) {
            val uid = sessionRepository.current()?.userId ?: return@withContext
            col.document(uid).delete().await()
        }
    }
}

private fun DocumentSnapshot?.toStatus(): DataExportStatus {
    if (this == null || !exists()) return DataExportStatus.Idle
    val status = getString("status") ?: return DataExportStatus.Pending
    return when (status) {
        "queued", "running" -> DataExportStatus.Pending
        "ready" -> {
            val url = getString("downloadUrl")
            val size = getLong("sizeBytes")
            if (url != null) DataExportStatus.Ready(downloadUrl = url, sizeBytes = size)
            else DataExportStatus.Pending
        }
        "failed" -> DataExportStatus.Failed(reason = getString("errorMessage"))
        else -> DataExportStatus.Pending
    }
}
