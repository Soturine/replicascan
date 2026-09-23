package com.soturine.replicascan.feature.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.OcrFailureReason
import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrScriptPolicy
import com.soturine.replicascan.core.common.model.OcrTextParagraph
import com.soturine.replicascan.core.common.model.OcrTextQuality
import com.soturine.replicascan.core.common.model.OcrTextResult
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.OcrRecognitionException
import com.soturine.replicascan.core.common.repository.OcrRepository
import com.soturine.replicascan.core.common.repository.OcrRequest
import com.soturine.replicascan.core.common.repository.ScanRepository
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OcrMessage {
    IMAGE_UNREADABLE,
    MODEL_PREPARING,
    FAILED,
}

data class OcrUiState(
    val isLoaded: Boolean = false,
    val page: ScanPage? = null,
    val text: String = "",
    val paragraphs: List<OcrTextParagraph> = emptyList(),
    val quality: OcrTextQuality = OcrTextQuality.EMPTY,
    val isRecognizing: Boolean = false,
    val script: OcrScript = OcrScript.LATIN,
    val readiness: OcrModelReadiness = OcrModelReadiness.READY,
    val message: OcrMessage? = null,
)

class OcrViewModel(
    private val scanId: String,
    private val pageId: String,
    private val scanRepository: ScanRepository,
    private val processingRepository: DocumentProcessingRepository,
    private val ocrRepository: OcrRepository,
    locale: Locale = Locale.getDefault(),
) : ViewModel() {
    private val freshResult = MutableStateFlow<OcrTextResult?>(null)
    private val status = MutableStateFlow(
        RecognitionStatus(script = OcrScriptPolicy.defaultFor(locale)),
    )
    private var recognitionJob: Job? = null
    private var generation = 0

    private val pageFlow = scanRepository.observeScan(scanId)
        .map { scan -> scan?.pages?.firstOrNull { it.id == pageId } }

    val uiState: StateFlow<OcrUiState> = combine(pageFlow, freshResult, status) { page, fresh, current ->
        // A fresh run wins; otherwise the text persisted for this page is shown without re-running OCR.
        val result = fresh ?: OcrTextResult.fromPlainText(page?.ocrText.orEmpty())
        OcrUiState(
            isLoaded = true,
            page = page,
            text = result.fullText.trim(),
            paragraphs = result.paragraphs,
            quality = result.quality,
            isRecognizing = current.running,
            script = current.script,
            readiness = current.readiness,
            message = current.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OcrUiState(),
    )

    init {
        viewModelScope.launch {
            val page = pageFlow.filterNotNull().first()
            if (page.ocrText.isNullOrBlank()) recognize(page, status.value.script)
        }
    }

    fun retry() {
        val page = uiState.value.page ?: return
        recognize(page, status.value.script)
    }

    fun selectScript(script: OcrScript) {
        val page = uiState.value.page ?: return
        recognize(page, script)
    }

    fun clearMessage() {
        status.value = status.value.copy(message = null)
    }

    private fun recognize(page: ScanPage, script: OcrScript) {
        if (recognitionJob?.isActive == true && status.value.script == script) return
        recognitionJob?.cancel()
        val runId = ++generation
        status.value = status.value.copy(script = script, running = true, message = null)
        recognitionJob = viewModelScope.launch {
            try {
                val readiness = ocrRepository.modelReadiness(script)
                status.value = status.value.copy(readiness = readiness)
                val preparedUri = processingRepository.processForOcr(page.sourceUri, page.quad, page.rotationDegrees)
                val result = ocrRepository.recognize(
                    OcrRequest(imageUri = preparedUri, script = script, sourceFingerprint = fingerprint(page, script)),
                )
                freshResult.value = result
                status.value = status.value.copy(readiness = OcrModelReadiness.READY)
                scanRepository.updatePageOcrArtifact(scanId, pageId, result)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: OcrRecognitionException) {
                status.value = status.value.copy(
                    message = when (exception.reason) {
                        OcrFailureReason.IMAGE_UNREADABLE -> OcrMessage.IMAGE_UNREADABLE
                        OcrFailureReason.MODEL_NOT_READY -> OcrMessage.MODEL_PREPARING
                        OcrFailureReason.RECOGNITION_FAILED -> OcrMessage.FAILED
                    },
                )
            } catch (_: Exception) {
                status.value = status.value.copy(message = OcrMessage.FAILED)
            } finally {
                // A cancelled older run must not clear the progress of the run that replaced it.
                if (runId == generation) status.value = status.value.copy(running = false)
            }
        }
    }

    private fun fingerprint(page: ScanPage, script: OcrScript): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                listOf(page.sourceUri, page.quad.toString(), page.rotationDegrees.toString(), script.name, OcrRequest.PIPELINE_VERSION)
                    .joinToString("|")
                    .toByteArray(),
            )
            .joinToString("") { byte -> "%02x".format(byte) }

    private data class RecognitionStatus(
        val script: OcrScript,
        val running: Boolean = false,
        val readiness: OcrModelReadiness = OcrModelReadiness.READY,
        val message: OcrMessage? = null,
    )
}
