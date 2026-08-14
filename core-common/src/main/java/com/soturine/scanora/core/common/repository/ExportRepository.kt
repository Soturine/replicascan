package com.soturine.scanora.core.common.repository

import com.soturine.scanora.core.common.model.ExportedFile
import com.soturine.scanora.core.common.model.ExportFormat
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.PdfPageSize

interface ExportRepository {
    suspend fun exportPdf(
        scan: ScanDocument,
        quality: PdfQuality,
    ): ExportedFile = exportPdf(scan, quality, PdfPageSize.AUTO)

    suspend fun exportPdf(
        scan: ScanDocument,
        quality: PdfQuality,
        pageSize: PdfPageSize,
    ): ExportedFile = exportPdf(scan, quality)

    suspend fun exportImages(
        scan: ScanDocument,
        format: ExportFormat,
    ): List<ExportedFile>
}
