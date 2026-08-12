package com.soturine.scanora.core.data.repository

import com.soturine.scanora.core.common.model.CreatedScan
import com.soturine.scanora.core.common.model.DeletionOutcome
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.data.files.ScanFileStore
import com.soturine.scanora.core.data.local.dao.ScanDao
import com.soturine.scanora.core.data.local.entity.PageEntity
import com.soturine.scanora.core.data.local.entity.ScanEntity
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DefaultScanRepository(
    private val scanDao: ScanDao,
    private val fileStore: ScanFileStore,
) : ScanRepository {
    override fun observeScans(query: String): Flow<List<ScanDocument>> =
        scanDao.observeScans(query.trim()).map { items ->
            items.map { it.asExternalModel() }
        }

    override fun observeScan(scanId: String): Flow<ScanDocument?> =
        scanDao.observeScan(scanId).map { item -> item?.asExternalModel() }

    override suspend fun getScan(scanId: String): ScanDocument? = withContext(Dispatchers.IO) {
        scanDao.getScanWithPages(scanId)?.asExternalModel()
    }

    override suspend fun createScan(
        title: String,
        mode: ScanMode,
        sourceUris: List<String>,
        tags: List<String>,
        isDraft: Boolean,
    ): CreatedScan = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val scanId = UUID.randomUUID().toString()
        val scan = ScanEntity(
            id = scanId,
            title = title,
            mode = mode.storageKey,
            tags = tags.joinToString("|"),
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            isDraft = isDraft,
        )
        val pages = sourceUris.mapIndexed { index, uri ->
            PageEntity(
                id = UUID.randomUUID().toString(),
                scanId = scanId,
                pageIndex = index,
                sourceUri = uri,
                processedUri = null,
                filterType = com.soturine.scanora.core.common.model.DocumentFilterType.ORIGINAL_CORRECTED.storageKey,
                rotationDegrees = 0,
                quad = null,
                ocrText = null,
            )
        }
        scanDao.insertScanWithPages(scan, pages)
        CreatedScan(
            scanId = scanId,
            pageIds = pages.map(PageEntity::id),
        )
    }

    override suspend fun addPage(scanId: String, sourceUri: String): String = withContext(Dispatchers.IO) {
        val existingPages = scanDao.getPages(scanId)
        val pageId = UUID.randomUUID().toString()
        scanDao.upsertPages(
            listOf(
                PageEntity(
                    id = pageId,
                    scanId = scanId,
                    pageIndex = existingPages.size,
                    sourceUri = sourceUri,
                    processedUri = null,
                    filterType = com.soturine.scanora.core.common.model.DocumentFilterType.ORIGINAL_CORRECTED.storageKey,
                    rotationDegrees = 0,
                    quad = null,
                    ocrText = null,
                ),
            ),
        )
        scanDao.touchScan(scanId, System.currentTimeMillis())
        pageId
    }

    override suspend fun updatePage(scanId: String, page: ScanPage) {
        withContext(Dispatchers.IO) {
            scanDao.updatePage(page.asEntity())
            scanDao.touchScan(scanId, System.currentTimeMillis())
        }
    }

    override suspend fun updatePageOrder(scanId: String, orderedPageIds: List<String>) {
        withContext(Dispatchers.IO) {
            val currentPages = scanDao.getPages(scanId).associateBy { it.id }
            val reordered = orderedPageIds.mapIndexedNotNull { index, id ->
                currentPages[id]?.copy(pageIndex = index)
            }
            if (reordered.isNotEmpty()) {
                scanDao.upsertPages(reordered)
                scanDao.touchScan(scanId, System.currentTimeMillis())
            }
        }
    }

    override suspend fun deletePage(scanId: String, pageId: String): DeletionOutcome = withContext(Dispatchers.IO) {
        val page = scanDao.getPage(pageId)
            ?.takeIf { it.scanId == scanId }
            ?: return@withContext DeletionOutcome(databaseDeleted = false)
        scanDao.deletePageAndReindex(scanId, pageId, System.currentTimeMillis())
        val cleanup = fileStore.deletePageFiles(page.sourceUri, page.processedUri)
        cleanup.copy(databaseDeleted = true)
    }

    override suspend fun renameScan(scanId: String, title: String) {
        updateScan(scanId) { entity ->
            entity.withChanges(
                title = title,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun updateTags(scanId: String, tags: List<String>) {
        updateScan(scanId) { entity ->
            entity.withChanges(
                tags = tags.joinToString("|"),
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun toggleFavorite(scanId: String) {
        updateScan(scanId) { entity ->
            entity.withChanges(
                isFavorite = !entity.isFavorite,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun updatePageOcr(scanId: String, pageId: String, text: String) {
        withContext(Dispatchers.IO) {
            val page = scanDao.getPages(scanId).firstOrNull { it.id == pageId } ?: return@withContext
            scanDao.updatePage(page.copy(ocrText = text))
            scanDao.touchScan(scanId, System.currentTimeMillis())
        }
    }

    override suspend fun markScanSaved(scanId: String) {
        updateScan(scanId) { entity ->
            entity.withChanges(
                updatedAt = System.currentTimeMillis(),
                isDraft = false,
            )
        }
    }

    override suspend fun deleteScan(scanId: String): DeletionOutcome = withContext(Dispatchers.IO) {
        val pages = scanDao.getPages(scanId)
        val existed = scanDao.getScanEntity(scanId) != null
        if (!existed) return@withContext DeletionOutcome(databaseDeleted = false)
        scanDao.deleteScan(scanId)
        val cleanup = fileStore.deleteScanFiles(pages.map { it.sourceUri to it.processedUri })
        cleanup.copy(databaseDeleted = true)
    }

    private suspend fun updateScan(
        scanId: String,
        transform: (ScanEntity) -> ScanEntity,
    ) {
        withContext(Dispatchers.IO) {
            val entity = scanDao.getScanEntity(scanId) ?: return@withContext
            scanDao.upsertScan(transform(entity))
        }
    }
}

