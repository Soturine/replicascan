package com.soturine.replicascan.feature.export

import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument

enum class ExportMessage {
    EMPTY_DOCUMENT,
    PAGE_UNREADABLE,
    WRITE_FAILED,
}

data class ExportUiState(
    val scan: ScanDocument? = null,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val selectedQuality: PdfQuality = PdfQuality.BALANCED,
    val selectedPageSize: PdfPageSize = PdfPageSize.AUTO,
    val isExporting: Boolean = false,
    val exportedFiles: List<ExportedFile> = emptyList(),
    val message: ExportMessage? = null,
    val failedPageNumber: Int? = null,
)
