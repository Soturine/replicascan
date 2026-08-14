package com.soturine.scanora.feature.export

import com.soturine.scanora.core.common.model.ExportedFile
import com.soturine.scanora.core.common.model.ExportFormat
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.PdfPageSize

data class ExportUiState(
    val scan: ScanDocument? = null,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val selectedQuality: PdfQuality = PdfQuality.BALANCED,
    val isExporting: Boolean = false,
    val exportedFiles: List<ExportedFile> = emptyList(),
    val errorMessage: String? = null,
    val selectedPageSize: PdfPageSize = PdfPageSize.AUTO,
)

