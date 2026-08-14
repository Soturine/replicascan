package com.soturine.scanora.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.soturine.scanora.core.data.local.ScanWithPages
import com.soturine.scanora.core.data.local.entity.PageEntity
import com.soturine.scanora.core.data.local.entity.ScanEntity
import com.soturine.scanora.core.data.local.entity.PageOcrArtifactEntity
import com.soturine.scanora.core.data.local.entity.ScanSearchFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Transaction
    @Query("SELECT * FROM scans ORDER BY updatedAt DESC")
    fun observeAllScans(): Flow<List<ScanWithPages>>

    @Transaction
    @Query(
        """
        SELECT scans.* FROM scans
        JOIN scan_search_fts ON scans.searchRowId = scan_search_fts.rowid
        WHERE scan_search_fts MATCH :query
        ORDER BY scans.updatedAt DESC
        """,
    )
    fun observeSearchScans(query: String): Flow<List<ScanWithPages>>

    @Transaction
    @Query("SELECT * FROM scans ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentScans(limit: Int): Flow<List<ScanWithPages>>

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :scanId")
    fun observeScan(scanId: String): Flow<ScanWithPages?>

    @Transaction
    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanWithPages(scanId: String): ScanWithPages?

    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanEntity(scanId: String): ScanEntity?

    @Query("SELECT * FROM pages WHERE scanId = :scanId ORDER BY pageIndex ASC")
    suspend fun getPages(scanId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getPage(pageId: String): PageEntity?

    @Query("SELECT * FROM pages")
    suspend fun getAllPages(): List<PageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScan(scan: ScanEntity)

    @Update
    suspend fun updateScan(scan: ScanEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOcrArtifact(artifact: PageOcrArtifactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearchEntry(entry: ScanSearchFtsEntity)

    @Query("DELETE FROM scan_search_fts WHERE rowid = :rowId")
    suspend fun deleteSearchEntry(rowId: Long)

    @Query("DELETE FROM page_ocr_artifacts WHERE pageId = :pageId")
    suspend fun deleteOcrArtifact(pageId: String)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePage(pageId: String)

    @Query("DELETE FROM scans WHERE id = :scanId")
    suspend fun deleteScan(scanId: String)

    @Query("UPDATE scans SET updatedAt = :updatedAt WHERE id = :scanId")
    suspend fun touchScan(scanId: String, updatedAt: Long)

    @Query("UPDATE pages SET processedUri = NULL WHERE id = :pageId")
    suspend fun clearProcessedUri(pageId: String)

    @Query("UPDATE pages SET pageIndex = :pageIndex WHERE id = :pageId")
    suspend fun setPageIndex(pageId: String, pageIndex: Int)

    @Transaction
    suspend fun insertScanWithPages(
        scan: ScanEntity,
        pages: List<PageEntity>,
    ) {
        insertScan(scan)
        upsertPages(pages)
    }

    @Transaction
    suspend fun insertScanWithPagesAndSearch(
        scan: ScanEntity,
        pages: List<PageEntity>,
        searchEntry: ScanSearchFtsEntity,
    ) {
        insertScan(scan)
        upsertPages(pages)
        upsertSearchEntry(searchEntry)
    }

    @Transaction
    suspend fun updateScanAndSearch(scan: ScanEntity, searchEntry: ScanSearchFtsEntity) {
        check(updateScan(scan) == 1)
        upsertSearchEntry(searchEntry)
    }

    @Transaction
    suspend fun updateOcrAndSearch(
        page: PageEntity,
        artifact: PageOcrArtifactEntity,
        searchEntry: ScanSearchFtsEntity,
        updatedAt: Long,
    ) {
        updatePage(page)
        upsertOcrArtifact(artifact)
        upsertSearchEntry(searchEntry)
        touchScan(page.scanId, updatedAt)
    }

    @Transaction
    suspend fun updatePageAndSearch(
        page: PageEntity,
        searchEntry: ScanSearchFtsEntity,
        invalidateOcr: Boolean,
        updatedAt: Long,
    ) {
        updatePage(page)
        if (invalidateOcr) deleteOcrArtifact(page.id)
        upsertSearchEntry(searchEntry)
        touchScan(page.scanId, updatedAt)
    }

    @Transaction
    suspend fun deletePageAndReindex(
        scanId: String,
        pageId: String,
        updatedAt: Long,
    ) {
        deletePage(pageId)
        val remainingPages = getPages(scanId)
        if (remainingPages.isEmpty()) {
            deleteScan(scanId)
        } else {
            reindexPages(remainingPages.map(PageEntity::id))
            touchScan(scanId, updatedAt)
        }
    }

    @Transaction
    suspend fun reindexPages(orderedPageIds: List<String>) {
        orderedPageIds.forEachIndexed { index, pageId -> setPageIndex(pageId, -(index + 1)) }
        orderedPageIds.forEachIndexed { index, pageId -> setPageIndex(pageId, index) }
    }
}

