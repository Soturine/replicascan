package com.soturine.replicascan.core.common.model

/**
 * Optional page looks. Storage keys are persisted in Room and must never change; retired
 * keys from earlier builds are read back as their closest current look.
 */
enum class DocumentFilterType(
    val storageKey: String,
) {
    ORIGINAL("original_corrected"),
    ENHANCED("color_enhanced"),
    GRAYSCALE("document_gray"),
    BLACK_WHITE("document_bw");

    companion object {
        fun fromStorageKey(value: String): DocumentFilterType =
            entries.firstOrNull { it.storageKey == value } ?: when (value) {
                LEGACY_RECEIPT_KEY -> BLACK_WHITE
                else -> ORIGINAL
            }

        private const val LEGACY_RECEIPT_KEY = "receipt_high_contrast"
    }
}
