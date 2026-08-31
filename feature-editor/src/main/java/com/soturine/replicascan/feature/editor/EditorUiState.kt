package com.soturine.replicascan.feature.editor

import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage

data class EditorUiState(
    val scan: ScanDocument? = null,
    val currentPage: ScanPage? = null,
    val isProcessing: Boolean = false,
    val isPreviewLoading: Boolean = false,
    val isPreviewRefining: Boolean = false,
    val previewImageUri: String? = null,
    val detectionResult: DocumentDetectionResult? = null,
    val errorMessage: String? = null,
)
