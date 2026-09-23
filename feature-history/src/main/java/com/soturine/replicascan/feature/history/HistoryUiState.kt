package com.soturine.replicascan.feature.history

import com.soturine.replicascan.core.common.model.ScanDocument

data class HistoryUiState(
    val isLoaded: Boolean = false,
    val query: String = "",
    val scans: List<ScanDocument> = emptyList(),
)

