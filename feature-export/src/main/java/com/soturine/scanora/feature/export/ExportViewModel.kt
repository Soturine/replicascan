package com.soturine.scanora.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.scanora.core.common.model.ExportFormat
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.repository.ExportRepository
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.common.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.cancellation.CancellationException
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
    private val isExporting = MutableStateFlow(false)
    private val exportedFiles = MutableStateFlow(emptyList<com.soturine.scanora.core.common.model.ExportedFile>())
    private val errorMessage = MutableStateFlow<String?>(null)

    private val exportSelection = combine(
        selectedFormat,
        selectedQuality,
    ) { format, qualityOverride ->
        format to qualityOverride
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
        val (format, qualityOverride) = selection
        val (exporting, files, message) = status
        ExportUiState(
            scan = scan,
            selectedFormat = format,
            selectedQuality = qualityOverride ?: preferences.defaultPdfQuality,
            isExporting = exporting,
            exportedFiles = files,
            errorMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExportUiState(),
    )

    fun selectFormat(format: ExportFormat) {
        selectedFormat.value = format
        exportedFiles.value = emptyList()
    }

    fun selectQuality(quality: PdfQuality) {
        selectedQuality.value = quality
        exportedFiles.value = emptyList()
    }

    fun export() {
        val scan = uiState.value.scan ?: return
        viewModelScope.launch {
            isExporting.value = true
            errorMessage.value = null
            exportedFiles.value = emptyList()
            try {
                try {
                    val files = if (uiState.value.selectedFormat == ExportFormat.PDF) {
                        listOf(
                            exportRepository.exportPdf(
                                scan = scan,
                                quality = uiState.value.selectedQuality,
                            ),
                        )
                    } else {
                        exportRepository.exportImages(scan, uiState.value.selectedFormat)
                    }
                    scanRepository.markScanSaved(scanId)
                    exportedFiles.value = files
                } catch (exception: CancellationException) {
                    throw exception
                } catch (throwable: Exception) {
                    errorMessage.value = throwable.message ?: "Não foi possível exportar o lote."
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
