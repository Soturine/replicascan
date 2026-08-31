package com.soturine.replicascan.core.common.model

data class CreatedScan(
    val scanId: String,
    val pageIds: List<String>,
)

data class DeletionOutcome(
    val databaseDeleted: Boolean,
    val deletedFileCount: Int = 0,
    val missingFileCount: Int = 0,
    val skippedExternalCount: Int = 0,
    val failedFileCount: Int = 0,
) {
    val hasCleanupFailures: Boolean
        get() = failedFileCount > 0
}
