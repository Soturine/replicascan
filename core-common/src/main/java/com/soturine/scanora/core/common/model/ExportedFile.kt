package com.soturine.scanora.core.common.model

data class ExportedFile(
    val displayName: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val locationLabel: String,
    val pathHint: String? = null,
    val searchableTextIncluded: Boolean = false,
)
