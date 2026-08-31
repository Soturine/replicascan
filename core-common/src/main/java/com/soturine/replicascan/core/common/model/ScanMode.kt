package com.soturine.replicascan.core.common.model

enum class ScanMode(
    val storageKey: String,
) {
    NOTEBOOK("notebook"),
    DOCUMENT("document"),
    RECEIPT("receipt");

    companion object {
        fun fromStorageKey(value: String): ScanMode = entries.firstOrNull { it.storageKey == value } ?: DOCUMENT
    }
}
