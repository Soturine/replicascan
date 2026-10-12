package com.soturine.replicascan.app

import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.*
import com.soturine.replicascan.core.common.repository.*
import com.soturine.replicascan.feature.export.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    private lateinit var vm: ExportViewModel
    private val scan = ScanDocument("scan", "Original", emptyList(), false, 0, 0,
        listOf(ScanPage("page", "scan", 0, "private-source")), true)
    private val file = ExportedFile("Renamed.pdf", "content://saved", "application/pdf", 100)

    @Test fun savedFilesRemainAvailableWhenLibraryUpdateFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = TestExportRepository(file)
            vm = ExportViewModel("scan", TestScans(scan, failSave = true), TestPreferences(), repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            runCurrent()
            vm.export("Renamed")
            runCurrent()
            assertEquals(listOf(file), vm.uiState.value.exportedFiles)
            assertEquals(ExportMessage.LIBRARY_UPDATE_FAILED, vm.uiState.value.message)
            assertFalse(vm.uiState.value.isExporting)
            assertEquals("Renamed", repo.receivedTitle)
            assertEquals("Original", scan.title)
        } finally { vm.viewModelScope.cancel(); backgroundScope.cancel(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun exportFailureNeverProducesSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val scans = TestScans(scan)
            val repo = TestExportRepository(file, failWrite = true)
            vm = ExportViewModel("scan", scans, TestPreferences(), repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            runCurrent()
            vm.export("Renamed")
            runCurrent()
            assertTrue(vm.uiState.value.exportedFiles.isEmpty())
            assertEquals(ExportMessage.WRITE_FAILED, vm.uiState.value.message)
            assertFalse(scans.markedSaved)
            assertFalse(vm.uiState.value.isExporting)
        } finally { vm.viewModelScope.cancel(); backgroundScope.cancel(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun activeExportFreezesSelectionAndRejectsDuplicateRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val finish = CompletableDeferred<Unit>()
            val repo = TestExportRepository(file, finish = finish)
            vm = ExportViewModel("scan", TestScans(scan), TestPreferences(), repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            runCurrent()
            vm.export("Renamed")
            runCurrent()
            assertTrue(vm.uiState.value.isExporting)
            vm.selectFormat(ExportFormat.PNG)
            vm.selectQuality(PdfQuality.HIGH)
            vm.export("Duplicate")
            runCurrent()
            assertEquals(ExportFormat.PDF, vm.uiState.value.selectedFormat)
            assertEquals(PdfQuality.BALANCED, vm.uiState.value.selectedQuality)
            assertEquals(1, repo.calls)
            finish.complete(Unit)
            runCurrent()
            assertEquals(listOf(file), vm.uiState.value.exportedFiles)
        } finally { vm.viewModelScope.cancel(); backgroundScope.cancel(); runCurrent(); Dispatchers.resetMain() }
    }

    private class TestExportRepository(val file: ExportedFile, val failWrite: Boolean = false,
        val finish: CompletableDeferred<Unit>? = null) : ExportRepository {
        var calls = 0
        var receivedTitle = ""
        override suspend fun exportPdf(scan: ScanDocument, quality: PdfQuality, pageSize: PdfPageSize): ExportedFile {
            calls++
            receivedTitle = scan.title
            finish?.await()
            if (failWrite) throw ExportException(ExportFailureReason.WRITE_FAILED)
            return file
        }
        override suspend fun exportImages(scan: ScanDocument, format: ExportFormat): List<ExportedFile> = error("Unexpected image export")
    }

    private class TestPreferences : UserPreferencesRepository {
        override val preferences = flowOf(UserPreferences())
        override suspend fun setOnboardingCompleted(completed: Boolean) = error("unused")
        override suspend fun setThemePreference(preference: AppThemePreference) = error("unused")
        override suspend fun setDefaultPdfQuality(quality: PdfQuality) = error("unused")
    }

    private class TestScans(val scan: ScanDocument, val failSave: Boolean = false) : ScanRepository {
        var markedSaved = false
        override fun observeScan(scanId: String) = flowOf(scan)
        override suspend fun markScanSaved(scanId: String) { if (failSave) error("Library unavailable"); markedSaved = true }
        override fun observeScans(query: String): Flow<List<ScanDocument>> = error("unused")
        override fun observeRecentScans(limit: Int): Flow<List<ScanDocument>> = error("unused")
        override suspend fun getScan(scanId: String): ScanDocument? = error("unused")
        override suspend fun createScan(title: String, sourceUris: List<String>, tags: List<String>, isDraft: Boolean): CreatedScan = error("unused")
        override suspend fun addPage(scanId: String, sourceUri: String): String = error("unused")
        override suspend fun updatePage(scanId: String, page: ScanPage) = error("unused")
        override suspend fun updatePageOrder(scanId: String, orderedPageIds: List<String>) = error("unused")
        override suspend fun deletePage(scanId: String, pageId: String): DeletionOutcome = error("unused")
        override suspend fun renameScan(scanId: String, title: String) = error("unused")
        override suspend fun updateTags(scanId: String, tags: List<String>) = error("unused")
        override suspend fun toggleFavorite(scanId: String) = error("unused")
        override suspend fun updatePageOcrArtifact(scanId: String, pageId: String, result: OcrTextResult) = error("unused")
        override suspend fun deleteScan(scanId: String): DeletionOutcome = error("unused")
    }
}
