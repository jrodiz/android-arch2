package com.rodiz.arch2.feature.settings.domain.model

/**
 * Lifecycle of a user-data export request. Written server-side by the
 * `onDataExportRequest` Cloud Function as it gathers + uploads the JSON blob;
 * mirrored on the client via [DataExportRepository.observeMyExport].
 */
sealed interface DataExportStatus {
    /** No request in flight, or the previous one has been cleared. */
    data object Idle : DataExportStatus

    /** Request doc written; function is gathering + uploading. */
    data object Pending : DataExportStatus

    /**
     * Function finished. [downloadUrl] is a signed URL (currently 7-day expiry);
     * [sizeBytes] is the JSON payload length surface-able as "~12 KB".
     */
    data class Ready(val downloadUrl: String, val sizeBytes: Long?) : DataExportStatus

    /** Function bailed before finishing. [reason] is best-effort from the catch block. */
    data class Failed(val reason: String?) : DataExportStatus
}
