package com.rodiz.arch2.feature.settings.domain.repository

import com.rodiz.arch2.feature.settings.domain.model.DataExportStatus
import kotlinx.coroutines.flow.Flow

/**
 * "Download my data" backend: client writes a request doc, the
 * `onDataExportRequest` Cloud Function gathers the user's data into a JSON
 * blob on Storage and writes a signed download URL back onto the same doc.
 * The presentation layer treats this as a single reactive status flow.
 */
interface DataExportRepository {
    /**
     * Live status of the user's most recent export request. Emits [DataExportStatus.Idle]
     * until [requestExport] is called for the first time, then walks through Pending →
     * Ready / Failed as the function progresses.
     */
    fun observeMyExport(): Flow<DataExportStatus>

    /**
     * Fire-and-forget kick-off. Writes the request doc; the status flow then
     * picks up the function's mutations. Safe to call repeatedly — each call
     * overwrites the previous request (and triggers a fresh function run).
     */
    suspend fun requestExport()

    /** Clear the cached status doc so the next observe starts at [DataExportStatus.Idle]. */
    suspend fun acknowledge()
}
