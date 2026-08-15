package com.soturine.scanora.core.data.repository

import com.soturine.scanora.core.common.model.CreatedScan
import com.soturine.scanora.core.common.model.DeletionOutcome
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.model.OcrTextResult
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.data.files.ScanFileStore
import com.soturine.scanora.core.data.local.dao.ScanDao
import com.soturine.scanora.core.data.local.entity.PageEntity
import com.soturine.scanora.core.data.local.entity.ScanEntity
import com.soturine.scanora.core.data.local.entity.PageOcrArtifactEntity
import com.soturine.scanora.core.data.local.entity.ScanSearchFtsEntity
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DefaultScanRepository(
    private val scanDao: ScanDao,
    private val fileStore: ScanFileStore,
) : ScanRepository {
    override fun observeScans(query: String): Flow<List<ScanDocument>> =
        (query.toFtsQuery().takeIf(String::isNotEmpty)?.let(scanDao::observeSearchScans)
            ?: scanDao.observeAllScans()).map { items ->
            items.map { it.asExternalModel() }
        }

    override fun observeRecentScans(limit: Int): Flow<List<ScanDocument>> =
        scanDao.observeRecentScans(limit.coerceAtLeast(1)).map { items -> items.map { it.asExternalModel() } }

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
            tags = TagCodec.encode(tags),
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            isDraft = isDraft,
            searchRowId = UUID.fromString(scanId).mostSignificantBits and Long.MAX_VALUE,
        )
        val pages = sourceUris.mapIndexed { index, uri ->
            PageEntity(
                id = UUID.randomUUID().toString(),
                scanId = scanId,
                pageIndex = index,
                sourceUri = uri,
                processedUri = null,
                filterType = com.soturine.scanora.core.common.model.DocumentFilterType.AUTO.storageKey,
                rotationDegrees = 0,
                quad = null,
                ocrText = null,
            )
        }
        scanDao.insertScanWithPagesAndSearch(scan, pages, scan.toSearchEntry(ocrText = ""))
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
                    filterType = com.soturine.scanora.core.common.model.DocumentFilterType.AUTO.storageKey,
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
            val scan = scanDao.getScanEntity(scanId) ?: return@withContext
            val pages = scanDao.getPages(scanId)
            val previous = pages.firstOrNull { it.id == page.id } ?: return@withContext
            val updated = page.asEntity()
            val allPages = pages.map { if (it.id == page.id) updated else it }
            scanDao.updatePageAndSearch(
                page = updated,
                searchEntry = scan.toSearchEntry(allPages.mapNotNull(PageEntity::ocrText).joinToString(" ")),
                invalidateOcr = previous.ocrText != null && updated.ocrText == null,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun updatePageOrder(scanId: String, orderedPageIds: List<String>) {
        withContext(Dispatchers.IO) {
            val pages = scanDao.getPages(scanId)
            val currentPages = pages.associateBy { it.id }
            require(orderedPageIds.size == pages.size && orderedPageIds.toSet().size == pages.size) {
                "Page order must contain every page exactly once."
            }
            require(orderedPageIds.toSet() == currentPages.keys) {
                "Page order must be an exact permutation of the current pages."
            }
            scanDao.reindexPages(orderedPageIds)
            scanDao.touchScan(scanId, System.currentTimeMillis())
        }
    }

    override suspend fun deletePage(scanId: String, pageId: String): DeletionOutcome = withContext(Dispatchers.IO) {
        val page = scanDao.getPage(pageId)
            ?.takeIf { it.scanId == scanId }
            ?: return@withContext DeletionOutcome(databaseDeleted = false)
        val scan = scanDao.getScanEntity(scanId)
        scanDao.deletePageAndReindex(scanId, pageId, System.currentTimeMillis())
        val remainingScan = scanDao.getScanEntity(scanId)
        if (remainingScan == null) {
            scan?.let { scanDao.deleteSearchEntry(it.searchRowId) }
        } else {
            val ocrText = scanDao.getPages(scanId).mapNotNull(PageEntity::ocrText).joinToString(" ")
            scanDao.upsertSearchEntry(remainingScan.toSearchEntry(ocrText))
        }
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
                tags = TagCodec.encode(tags),
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

    override suspend fun updatePageOcrArtifact(scanId: String, pageId: String, result: OcrTextResult) {
        withContext(Dispatchers.IO) {
            val scan = scanDao.getScanEntity(scanId) ?: return@withContext
            val pages = scanDao.getPages(scanId)
            val page = pages.firstOrNull { it.id == pageId } ?: return@withContext
            val metadata = result.metadata
            val normalizedText = result.fullText
            val artifact = PageOcrArtifactEntity(
                pageId = pageId,
                rawText = result.fallbackText,
                normalizedText = normalizedText,
                structuredContent = result.toStructuredJson(),
                script = metadata?.script?.name ?: "LATIN",
                engine = metadata?.engine ?: "legacy",
                engineVersion = metadata?.engineVersion ?: "unknown",
                pipelineVersion = metadata?.pipelineVersion ?: "unknown",
                sourceFingerprint = metadata?.sourceFingerprint ?: page.sourceUri,
                createdAt = metadata?.createdAtEpochMillis ?: System.currentTimeMillis(),
            )
            val updatedPages = pages.map { entity -> if (entity.id == pageId) entity.copy(ocrText = normalizedText) else entity }
            val searchEntry = scan.toSearchEntry(updatedPages.mapNotNull(PageEntity::ocrText).joinToString(" "))
            scanDao.updateOcrAndSearch(
                page = page.copy(ocrText = normalizedText),
                artifact = artifact,
                searchEntry = searchEntry,
                updatedAt = System.currentTimeMillis(),
            )
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
        scanDao.getScanEntity(scanId)?.let { scanDao.deleteSearchEntry(it.searchRowId) }
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
            val updated = transform(entity)
            val ocrText = scanDao.getPages(scanId).mapNotNull(PageEntity::ocrText).joinToString(" ")
            scanDao.updateScanAndSearch(updated, updated.toSearchEntry(ocrText))
        }
    }

    private fun ScanEntity.toSearchEntry(ocrText: String) = ScanSearchFtsEntity(
        rowId = searchRowId,
        scanId = id,
        title = title,
        tags = TagCodec.decode(tags).joinToString(" "),
        ocrText = ocrText,
    )

    private fun String.toFtsQuery(): String =
        trim().split(Regex("[^\\p{L}\\p{N}_]+"))
            .filter(String::isNotBlank)
            .joinToString(" AND ") { token -> "${token.replace("\"", "\"\"")}*" }

    private fun OcrTextResult.toStructuredJson(): String = JSONObject().apply {
        put("blocks", JSONArray().apply {
            blocks.forEach { block ->
                put(JSONObject().apply {
                    put("text", block.text)
                    put("lines", JSONArray().apply {
                        block.lines.forEach { line ->
                            put(JSONObject().apply {
                                put("text", line.text)
                                line.confidence?.let { put("confidence", it) }
                                line.bounds?.let { bounds ->
                                    put("bounds", JSONArray(listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)))
                                }
                            })
                        }
                    })
                })
            }
        })
    }.toString()
}

