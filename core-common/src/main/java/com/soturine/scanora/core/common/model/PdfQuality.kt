package com.soturine.scanora.core.common.model

enum class PdfQuality(
    val storageKey: String,
    val jpegQuality: Int,
) {
    COMPACT(
        storageKey = "compact",
        jpegQuality = 70,
    ),
    BALANCED(
        storageKey = "balanced",
        jpegQuality = 84,
    ),
    HIGH(
        storageKey = "high",
        jpegQuality = 95,
    );

    companion object {
        fun fromStorageKey(value: String): PdfQuality =
            entries.firstOrNull { it.storageKey == value } ?: BALANCED
    }
}

