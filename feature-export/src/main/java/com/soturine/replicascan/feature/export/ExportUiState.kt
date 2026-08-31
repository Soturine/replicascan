package com.soturine.replicascan.feature.export

import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.PdfPageSize

data class ExportUiState(
    val scan: ScanDocument? = null,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val selectedQuality: PdfQuality = PdfQuality.BALANCED,
    val isExporting: Boolean = false,
    val exportedFiles: List<ExportedFile> = emptyList(),
    val errorMessage: String? = null,
    val selectedPageSize: PdfPageSize = PdfPageSize.AUTO,
)

