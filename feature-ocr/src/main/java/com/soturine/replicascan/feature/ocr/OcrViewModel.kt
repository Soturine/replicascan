package com.soturine.replicascan.feature.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.OcrTextResult
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.OcrRepository
import com.soturine.replicascan.core.common.repository.OcrRequest
import java.security.MessageDigest
import java.util.Locale
import com.soturine.replicascan.core.common.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OcrViewModel(
    private val scanId: String,
    private val pageId: String,
    private val scanRepository: ScanRepository,
    private val processingRepository: DocumentProcessingRepository,
    private val ocrRepository: OcrRepository,
) : ViewModel() {
    private val recognizedResult = MutableStateFlow(OcrTextResult.Empty)
    private val previewImageUri = MutableStateFlow<String?>(null)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val selectedScript = MutableStateFlow(OcrScript.AUTOMATIC)
    private val modelReadiness = MutableStateFlow(OcrModelReadiness.DOWNLOAD_PENDING)

    private val baseUiState = combine(
        scanRepository.observeScan(scanId),
        previewImageUri,
        recognizedResult,
        isLoading,
        errorMessage,
    ) { scan, previewUri, result, loading, message ->
        val page = scan?.pages?.firstOrNull { it.id == pageId }
        val resolvedResult = if (result.fullText.isBlank()) {
            OcrTextResult.fromPlainText(page?.ocrText.orEmpty())
        } else {
            result
        }
        OcrUiState(
            page = page,
            previewImageUri = previewUri ?: page?.displayUri,
            text = resolvedResult.fullText,
            paragraphs = resolvedResult.paragraphs,
            quality = resolvedResult.quality,
            discardedNoiseCount = resolvedResult.processedText.discardedNoiseCount,
            isLoading = loading,
            errorMessage = message,
        )
    }

    val uiState: StateFlow<OcrUiState> = combine(baseUiState, selectedScript, modelReadiness) { state, script, readiness ->
        state.copy(script = script, modelReadiness = readiness)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OcrUiState(),
    )

    init {
        viewModelScope.launch {
            val page = scanRepository.observeScan(scanId)
                .map { scan -> scan?.pages?.firstOrNull { it.id == pageId } }
                .filterNotNull()
                .first()
            runRecognition(page)
        }
    }

    fun recognize() {
        val page = uiState.value.page ?: return
        viewModelScope.launch {
            runRecognition(page)
        }
    }

    fun selectScript(script: OcrScript) {
        if (script == selectedScript.value) return
        selectedScript.value = script
        recognizedResult.value = OcrTextResult.Empty
        val page = uiState.value.page ?: return
        viewModelScope.launch { runRecognition(page) }
    }

    fun clearMessage() {
        errorMessage.value = null
    }

    private suspend fun runRecognition(page: ScanPage) {
        isLoading.value = true
        errorMessage.value = null
        runCatching {
            modelReadiness.value = ocrRepository.modelReadiness(selectedScript.value)
            val preparedUri = processingRepository.processForOcr(
                sourceUri = page.sourceUri,
                quad = page.quad,
                rotationDegrees = page.rotationDegrees,
                preferReceiptMode = page.filterType == DocumentFilterType.RECEIPT_HIGH_CONTRAST,
            )
            previewImageUri.value = preparedUri
            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(
                    listOf(
                        page.sourceUri,
                        page.quad.toString(),
                        page.rotationDegrees.toString(),
                        page.filterType.name,
                        selectedScript.value.name,
                        localeScriptHint()?.name.orEmpty(),
                        "ocr-v3",
                    ).joinToString("|").toByteArray(),
                )
                .joinToString("") { byte -> "%02x".format(byte) }
            val result = ocrRepository.recognize(
                OcrRequest(
                    imageUri = preparedUri,
                    script = selectedScript.value,
                    fallbackHint = localeScriptHint(),
                    sourceFingerprint = fingerprint,
                ),
            )
            modelReadiness.value = ocrRepository.modelReadiness(selectedScript.value)
            recognizedResult.value = result
            scanRepository.updatePageOcrArtifact(scanId, pageId, result)
        }.onFailure { throwable ->
            errorMessage.value = throwable.message ?: "Não foi possível reconhecer o texto."
        }
        isLoading.value = false
    }

    private fun localeScriptHint(): OcrScript? = when (Locale.getDefault().language) {
        "hi", "mr", "ne" -> OcrScript.DEVANAGARI
        "ja" -> OcrScript.JAPANESE
        "ko" -> OcrScript.KOREAN
        else -> null
    }
}
