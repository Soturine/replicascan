package com.soturine.replicascan.core.common.usecase

import com.soturine.replicascan.core.common.model.ScanDocument

class SearchScansUseCase {
    operator fun invoke(
        scans: List<ScanDocument>,
        query: String,
    ): List<ScanDocument> {
        if (query.isBlank()) return scans
        val normalizedQuery = query.trim().lowercase()
        return scans.filter { scan ->
            scan.title.lowercase().contains(normalizedQuery) ||
                scan.tags.any { it.lowercase().contains(normalizedQuery) }
        }
    }
}

