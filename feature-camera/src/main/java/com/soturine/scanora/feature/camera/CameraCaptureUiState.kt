package com.soturine.scanora.feature.camera

import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.DocumentDetectionConfidence
import com.soturine.scanora.core.common.model.DocumentQuad

data class CameraCaptureUiState(
    val mode: ScanMode,
    val permissionGranted: Boolean = false,
    val isCapturing: Boolean = false,
    val capturedUris: List<String> = emptyList(),
    val liveQuad: DocumentQuad? = null,
    val liveConfidence: DocumentDetectionConfidence = DocumentDetectionConfidence.NONE,
    val errorMessage: String? = null,
)

