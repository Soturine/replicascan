package com.soturine.replicascan.core.common.repository

import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.CreatedScan
import com.soturine.replicascan.core.common.model.DeletionOutcome
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.ScanMode
import kotlinx.coroutines.flow.Flow
import com.soturine.replicascan.core.common.model.OcrTextResult

interface ScanRepository {
    fun observeScans(query: String = ""): Flow<List<ScanDocument>>

    fun observeRecentScans(limit: Int): Flow<List<ScanDocument>>

    fun observeScan(scanId: String): Flow<ScanDocument?>

    suspend fun getScan(scanId: String): ScanDocument?

    suspend fun createScan(
        title: String,
        mode: ScanMode,
        sourceUris: List<String>,
        tags: List<String> = emptyList(),
        isDraft: Boolean = true,
    ): CreatedScan

    suspend fun addPage(scanId: String, sourceUri: String): String

    suspend fun updatePage(scanId: String, page: ScanPage)

    suspend fun updatePageOrder(scanId: String, orderedPageIds: List<String>)

    suspend fun deletePage(scanId: String, pageId: String): DeletionOutcome

    suspend fun renameScan(scanId: String, title: String)

    suspend fun updateTags(scanId: String, tags: List<String>)

    suspend fun toggleFavorite(scanId: String)

    suspend fun updatePageOcr(scanId: String, pageId: String, text: String)

    suspend fun updatePageOcrArtifact(scanId: String, pageId: String, result: OcrTextResult) {
        updatePageOcr(scanId, pageId, result.fullText)
    }

    suspend fun markScanSaved(scanId: String)

    suspend fun deleteScan(scanId: String): DeletionOutcome
}

