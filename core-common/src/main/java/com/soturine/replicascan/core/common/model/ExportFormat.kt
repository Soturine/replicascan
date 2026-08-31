package com.soturine.replicascan.core.common.model

enum class ExportFormat(
    val storageKey: String,
    val title: String,
    val mimeType: String,
    val fileExtension: String,
) {
    PDF(
        storageKey = "pdf",
        title = "PDF",
        mimeType = "application/pdf",
        fileExtension = "pdf",
    ),
    JPG(
        storageKey = "jpg",
        title = "JPG",
        mimeType = "image/jpeg",
        fileExtension = "jpg",
    ),
    PNG(
        storageKey = "png",
        title = "PNG",
        mimeType = "image/png",
        fileExtension = "png",
    );

    companion object {
        fun fromStorageKey(value: String): ExportFormat = entries.firstOrNull { it.storageKey == value } ?: PDF
    }
}

