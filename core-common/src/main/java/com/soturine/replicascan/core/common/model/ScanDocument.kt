package com.soturine.replicascan.core.common.model

data class ScanDocument(
    val id: String,
    val title: String,
    val mode: ScanMode,
    val tags: List<String>,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<ScanPage>,
    val isDraft: Boolean,
) {
    val pageCount: Int
        get() = pages.size

    val coverPage: ScanPage?
        get() = pages.minByOrNull { it.index }
}

