package com.soturine.replicascan.core.common.model

data class ExportedFile(
    val displayName: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Absolute path only for app-storage exports (API < 29); MediaStore exports use [uri]. */
    val pathHint: String? = null,
    val searchableTextIncluded: Boolean = false,
)
