package com.soturine.replicascan.feature.home

import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanMode

data class HomeUiState(
    val isLoading: Boolean = true,
    val manualMode: ScanMode = ScanMode.DOCUMENT,
    val recentScans: List<ScanDocument> = emptyList(),
)

