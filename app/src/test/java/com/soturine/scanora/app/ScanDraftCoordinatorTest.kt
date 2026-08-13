package com.soturine.scanora.app

import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.common.model.CreatedScan
import com.soturine.scanora.core.common.model.DeletionOutcome
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.data.files.ImportFailure
import com.soturine.scanora.core.data.files.ImportFailureReason
import com.soturine.scanora.core.data.files.ImportedSource
import com.soturine.scanora.core.data.files.SourceFileStore
import com.soturine.scanora.core.data.files.SourceImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ScanDraftCoordinatorTest {
    @Test
    fun partialImportPersistsSuccessfulSourcesInInputOrder() = runTest {
        val fileStore = FakeSourceFileStore(
            SourceImportResult(
                imported = listOf(ImportedSource(0, "first"), ImportedSource(2, "third")),
                failures = listOf(ImportFailure(1, ImportFailureReason.UNAVAILABLE_SOURCE)),
            ),
        )
        val repository = FakeScanRepository()
        val coordinator = ScanDraftCoordinator(repository, fileStore)

        val result = coordinator.createDraft(
            ScanMode.DOCUMENT,
            listOf("one", "two", "three"),
            DraftSource.MANUAL_IMPORT,
            "Imported document",
        )

        assertThat(repository.createdSourceUris).containsExactly("first", "third").inOrder()
        assertThat(result).isEqualTo(DraftCreationResult.Success("scan", "page-1", 2, 1))
    }

    @Test
    fun persistenceFailureRollsBackEveryCopiedSource() = runTest {
        val imported = listOf(ImportedSource(0, "first"), ImportedSource(1, "second"))
        val fileStore = FakeSourceFileStore(SourceImportResult(imported, emptyList()))
        val repository = FakeScanRepository(failCreate = true)
        val coordinator = ScanDraftCoordinator(repository, fileStore)

        val result = coordinator.createDraft(
            ScanMode.DOCUMENT,
            listOf("one", "two"),
            DraftSource.QUICK_SCAN,
            "Scanned document",
        )

        assertThat(fileStore.rolledBack).containsExactlyElementsIn(imported)
        assertThat(result).isInstanceOf(DraftCreationResult.Failure::class.java)
    }

    private class FakeSourceFileStore(
        private val result: SourceImportResult,
    ) : SourceFileStore {
        var rolledBack = emptyList<ImportedSource>()

        override suspend fun importSources(uriValues: List<String>): SourceImportResult = result

        override suspend fun rollback(importedSources: List<ImportedSource>): DeletionOutcome {
            rolledBack = importedSources
            return DeletionOutcome(databaseDeleted = false, deletedFileCount = importedSources.size)
        }
    }

    private class FakeScanRepository(
        private val failCreate: Boolean = false,
    ) : ScanRepository {
        var createdSourceUris = emptyList<String>()

        override fun observeScans(query: String): Flow<List<ScanDocument>> = emptyFlow()
        override fun observeRecentScans(limit: Int): Flow<List<ScanDocument>> = emptyFlow()
        override fun observeScan(scanId: String): Flow<ScanDocument?> = emptyFlow()
        override suspend fun getScan(scanId: String): ScanDocument? = null

        override suspend fun createScan(
            title: String,
            mode: ScanMode,
            sourceUris: List<String>,
            tags: List<String>,
            isDraft: Boolean,
        ): CreatedScan {
            if (failCreate) error("database unavailable")
            createdSourceUris = sourceUris
            return CreatedScan("scan", listOf("page-1", "page-2"))
        }

        override suspend fun addPage(scanId: String, sourceUri: String): String = error("unused")
        override suspend fun updatePage(scanId: String, page: ScanPage) = Unit
        override suspend fun updatePageOrder(scanId: String, orderedPageIds: List<String>) = Unit
        override suspend fun deletePage(scanId: String, pageId: String) = DeletionOutcome(false)
        override suspend fun renameScan(scanId: String, title: String) = Unit
        override suspend fun updateTags(scanId: String, tags: List<String>) = Unit
        override suspend fun toggleFavorite(scanId: String) = Unit
        override suspend fun updatePageOcr(scanId: String, pageId: String, text: String) = Unit
        override suspend fun markScanSaved(scanId: String) = Unit
        override suspend fun deleteScan(scanId: String) = DeletionOutcome(false)
    }
}
