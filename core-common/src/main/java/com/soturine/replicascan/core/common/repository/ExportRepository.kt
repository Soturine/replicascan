package com.soturine.replicascan.core.common.repository

import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument

enum class ExportFailureReason {
    EMPTY_DOCUMENT,
    PAGE_UNREADABLE,
    WRITE_FAILED,
}

/** Export is all-or-nothing: any failure removes files already written for the same request. */
class ExportException(
    val reason: ExportFailureReason,
    val pageNumber: Int? = null,
    val pageId: String? = null,
    cause: Throwable? = null,
) : IllegalStateException(reason.name + (pageNumber?.let { " (page $it)" } ?: ""), cause)

interface ExportRepository {
    suspend fun exportPdf(
        scan: ScanDocument,
        quality: PdfQuality,
        pageSize: PdfPageSize = PdfPageSize.AUTO,
    ): ExportedFile

    suspend fun exportImages(
        scan: ScanDocument,
        format: ExportFormat,
    ): List<ExportedFile>
}
