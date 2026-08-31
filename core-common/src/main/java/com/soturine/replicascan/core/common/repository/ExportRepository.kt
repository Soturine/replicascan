package com.soturine.replicascan.core.common.repository

import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.PdfPageSize

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
