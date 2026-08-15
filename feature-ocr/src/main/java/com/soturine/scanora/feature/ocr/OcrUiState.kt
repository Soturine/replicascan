package com.soturine.scanora.feature.ocr

import com.soturine.scanora.core.common.model.OcrTextParagraph
import com.soturine.scanora.core.common.model.OcrTextQuality
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.model.OcrScript
import com.soturine.scanora.core.common.model.OcrModelReadiness

data class OcrUiState(
    val page: ScanPage? = null,
    val previewImageUri: String? = null,
    val text: String = "",
    val paragraphs: List<OcrTextParagraph> = emptyList(),
    val quality: OcrTextQuality = OcrTextQuality.EMPTY,
    val discardedNoiseCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val script: OcrScript = OcrScript.AUTOMATIC,
    val modelReadiness: OcrModelReadiness = OcrModelReadiness.DOWNLOAD_PENDING,
)

