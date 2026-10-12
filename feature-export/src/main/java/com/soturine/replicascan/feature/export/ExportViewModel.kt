package com.soturine.replicascan.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.repository.ExportException
import com.soturine.replicascan.core.common.repository.ExportFailureReason
import com.soturine.replicascan.core.common.repository.ExportRepository
import com.soturine.replicascan.core.common.repository.ScanRepository
import com.soturine.replicascan.core.common.repository.UserPreferencesRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExportViewModel(
    private val scanId: String,
    private val scanRepository: ScanRepository,
    preferencesRepository: UserPreferencesRepository,
    private val exportRepository: ExportRepository,
) : ViewModel() {
    private val selectedFormat = MutableStateFlow(ExportFormat.PDF)
    private val selectedQuality = MutableStateFlow<PdfQuality?>(null)
    private val selectedPageSize = MutableStateFlow(PdfPageSize.AUTO)
    private val isExporting = MutableStateFlow(false)
    private val exportedFiles = MutableStateFlow(emptyList<ExportedFile>())
    private val failedPage = MutableStateFlow<Int?>(null)
    private val errorMessage = MutableStateFlow<ExportMessage?>(null)

    private val exportSelection = combine(
        selectedFormat,
        selectedQuality,
        selectedPageSize,
    ) { format, qualityOverride, pageSize ->
        Triple(format, qualityOverride, pageSize)
    }

    private val exportStatus = combine(
        isExporting,
        exportedFiles,
        errorMessage,
    ) { exporting, files, message ->
        Triple(exporting, files, message)
    }

    val uiState: StateFlow<ExportUiState> = combine(
        scanRepository.observeScan(scanId),
        preferencesRepository.preferences,
        exportSelection,
        exportStatus,
    ) { scan, preferences, selection, status ->
        val (format, qualityOverride, pageSize) = selection
        val (exporting, files, message) = status
        ExportUiState(
            scan = scan,
            selectedFormat = format,
            selectedQuality = qualityOverride ?: preferences.defaultPdfQuality,
            isExporting = exporting,
            exportedFiles = files,
            message = message,
            failedPageNumber = failedPage.value,
            selectedPageSize = pageSize,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExportUiState(),
    )

    fun selectFormat(format: ExportFormat) {
        if (isExporting.value) return
        selectedFormat.value = format
        exportedFiles.value = emptyList()
    }

    fun selectQuality(quality: PdfQuality) {
        if (isExporting.value) return
        selectedQuality.value = quality
        exportedFiles.value = emptyList()
    }

    fun selectPageSize(pageSize: PdfPageSize) {
        if (isExporting.value) return
        selectedPageSize.value = pageSize
        exportedFiles.value = emptyList()
    }

    fun export(fileName: String) {
        val validation = com.soturine.replicascan.core.common.usecase.ValidateDocumentNameUseCase()(fileName)
        if (!validation.isValid) return
        val selection = uiState.value
        val scan = selection.scan?.copy(title = validation.sanitizedValue) ?: return
        if (isExporting.value) return
        isExporting.value = true
        viewModelScope.launch {
            errorMessage.value = null
            failedPage.value = null
            exportedFiles.value = emptyList()
            try {
                try {
                    val files = if (selection.selectedFormat == ExportFormat.PDF) {
                        listOf(
                            exportRepository.exportPdf(
                                scan = scan,
                                quality = selection.selectedQuality,
                                pageSize = selection.selectedPageSize,
                            ),
                        )
                    } else {
                        exportRepository.exportImages(scan, selection.selectedFormat)
                    }
                    exportedFiles.value = files
                    // Output files are already committed. A library failure must not hide them
                    // or claim that the export rolled back.
                    try {
                        scanRepository.markScanSaved(scanId)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        errorMessage.value = ExportMessage.LIBRARY_UPDATE_FAILED
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: ExportException) {
                    // Set before the message so the combined state never pairs a message with a stale page.
                    failedPage.value = exception.pageNumber
                    errorMessage.value = when (exception.reason) {
                        ExportFailureReason.EMPTY_DOCUMENT -> ExportMessage.EMPTY_DOCUMENT
                        ExportFailureReason.PAGE_UNREADABLE -> ExportMessage.PAGE_UNREADABLE
                        ExportFailureReason.WRITE_FAILED -> ExportMessage.WRITE_FAILED
                    }
                } catch (_: Exception) {
                    errorMessage.value = ExportMessage.WRITE_FAILED
                }
            } finally {
                isExporting.value = false
            }
        }
    }

    fun clearMessage() {
        errorMessage.value = null
    }
}
