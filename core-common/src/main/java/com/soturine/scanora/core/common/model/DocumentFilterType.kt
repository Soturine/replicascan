package com.soturine.scanora.core.common.model

enum class DocumentFilterType(
    val storageKey: String,
) {
    AUTO("auto"),
    ORIGINAL_CORRECTED("original_corrected"),
    DOCUMENT_BLACK_WHITE("document_bw"),
    DOCUMENT_GRAY("document_gray"),
    COLOR_ENHANCED("color_enhanced"),
    RECEIPT_HIGH_CONTRAST("receipt_high_contrast");

    companion object {
        fun fromStorageKey(value: String): DocumentFilterType =
            entries.firstOrNull { it.storageKey == value } ?: ORIGINAL_CORRECTED
    }
}

