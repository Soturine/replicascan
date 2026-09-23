package com.soturine.replicascan.feature.home

import com.soturine.replicascan.core.common.model.ScanDocument

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentScans: List<ScanDocument> = emptyList(),
)
