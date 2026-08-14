package com.soturine.scanora.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.DocumentDetectionResult
import com.soturine.scanora.core.common.model.DocumentProfile
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.ImagePipelineSpec
import com.soturine.scanora.core.common.model.PageRenderPurpose
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.requiresDerivedImage
import com.soturine.scanora.core.common.model.withInvalidatedDerivedImage
import com.soturine.scanora.core.common.repository.DocumentProcessingRepository
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.common.usecase.ValidateDocumentNameUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val scanId: String,
    initialPageId: String?,
    private val scanRepository: ScanRepository,
    private val processingRepository: DocumentProcessingRepository,
    private val validateDocumentNameUseCase: ValidateDocumentNameUseCase = ValidateDocumentNameUseCase(),
) : ViewModel() {
    private val selectedPageId = MutableStateFlow(initialPageId)
    private val isProcessing = MutableStateFlow(false)
    private val isPreviewLoading = MutableStateFlow(false)
    private val isPreviewRefining = MutableStateFlow(false)
    private val previewImageUri = MutableStateFlow<String?>(null)
    private val detectionResult = MutableStateFlow<DocumentDetectionResult?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val previewCache = mutableMapOf<String, String>()
    private var previewJob: Job? = null
    private var quadJob: Job? = null

    private data class EditorBaseState(
        val scan: com.soturine.scanora.core.common.model.ScanDocument?,
        val pageId: String?,
        val isProcessing: Boolean,
        val isPreviewLoading: Boolean,
        val isPreviewRefining: Boolean,
    )

    private val baseState = combine(
        scanRepository.observeScan(scanId),
        selectedPageId,
        isProcessing,
        isPreviewLoading,
        isPreviewRefining,
    ) { scan, pageId, processing, previewLoading, previewRefining ->
        EditorBaseState(
            scan = scan,
            pageId = pageId,
            isProcessing = processing,
            isPreviewLoading = previewLoading,
            isPreviewRefining = previewRefining,
        )
    }

    val uiState: StateFlow<EditorUiState> = combine(
        baseState,
        previewImageUri,
        detectionResult,
        errorMessage,
    ) { base, previewUri, detection, message ->
        val resolvedPage = base.scan?.pages
            ?.sortedBy { it.index }
            ?.firstOrNull { it.id == base.pageId }
            ?: base.scan?.pages?.sortedBy { it.index }?.firstOrNull()
        EditorUiState(
            scan = base.scan,
            currentPage = resolvedPage,
            isProcessing = base.isProcessing,
            isPreviewLoading = base.isPreviewLoading,
            isPreviewRefining = base.isPreviewRefining,
            previewImageUri = previewUri ?: resolvedPage?.displayUri,
            detectionResult = detection,
            errorMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState(),
    )

    fun selectPage(pageId: String) {
        previewJob?.cancel()
        selectedPageId.value = pageId
        isPreviewLoading.value = false
        isPreviewRefining.value = false
        previewImageUri.value = null
        detectionResult.value = null
        previewCache.clear()
    }

    fun ensureQuadForCurrentPage(force: Boolean = false) {
        val page = uiState.value.currentPage ?: return
        if (!force && page.quad != null) return
        if (quadJob?.isActive == true) return

        quadJob = viewModelScope.launch {
            isProcessing.value = true
            errorMessage.value = null
            runCatching {
                val detection = processingRepository.detectDocument(
                    imageUri = page.sourceUri,
                    profile = uiState.value.scan?.mode.toDocumentProfile(),
                )
                detectionResult.value = detection
                scanRepository.updatePage(
                    scanId = scanId,
                    page = page.copy(quad = detection.quadOrFullPage()).withInvalidatedDerivedImage(),
                )
            }.onFailure { throwable ->
                if (throwable !is CancellationException) {
                    errorMessage.value = throwable.message ?: "Não foi possível sugerir os cantos do documento."
                }
            }
            isProcessing.value = false
        }
    }

    fun updateQuad(
        quad: DocumentQuad,
        onSaved: (() -> Unit)? = null,
    ) {
        val page = uiState.value.currentPage ?: return
        previewJob?.cancel()
        isPreviewLoading.value = false
        isPreviewRefining.value = false
        previewImageUri.value = null
        previewCache.clear()
        viewModelScope.launch {
            scanRepository.updatePage(
                scanId = scanId,
                page = page.copy(quad = quad).withInvalidatedDerivedImage(),
            )
            onSaved?.invoke()
        }
    }

    fun reestimateCurrentPageQuad() {
        ensureQuadForCurrentPage(force = true)
    }

    fun prepareFilterPreview(
        filterType: DocumentFilterType,
        previewLongSide: Int,
    ) {
        val page = uiState.value.currentPage ?: return
        previewJob?.cancel()

        val targetDimension = previewLongSide.coerceIn(1400, 1800)
        val quickDimension = (targetDimension * 0.74f).toInt().coerceIn(1120, 1400)
        val currentDisplayUri = page.displayUri

        val refinedCacheKey = buildPreviewCacheKey(page, filterType, targetDimension)
        previewCache[refinedCacheKey]?.let { cachedPreview ->
            previewImageUri.value = cachedPreview
            isPreviewLoading.value = false
            isPreviewRefining.value = false
            return
        }

        val quickCacheKey = buildPreviewCacheKey(page, filterType, quickDimension)
        previewCache[quickCacheKey]?.let { cachedPreview ->
            previewImageUri.value = cachedPreview
            isPreviewLoading.value = false
            isPreviewRefining.value = quickCacheKey != refinedCacheKey
        } ?: run {
            isPreviewLoading.value = true
            isPreviewRefining.value = false
        }

        previewJob = viewModelScope.launch {
            errorMessage.value = null
            try {
                delay(150)
                val effectiveQuad = ensureQuad(page)
                if (previewCache[quickCacheKey] == null) {
                    val quickPreview = processingRepository.renderPreview(
                        sourceUri = page.sourceUri,
                        filterType = filterType,
                        quad = effectiveQuad,
                        rotationDegrees = page.rotationDegrees,
                        maxDimension = quickDimension,
                    )
                    previewCache[quickCacheKey] = quickPreview
                    previewImageUri.value = quickPreview
                    isPreviewLoading.value = false
                }

                isPreviewRefining.value = quickCacheKey != refinedCacheKey
                val refinedPreview = previewCache[refinedCacheKey] ?: processingRepository.renderPreview(
                    sourceUri = page.sourceUri,
                    filterType = filterType,
                    quad = effectiveQuad,
                    rotationDegrees = page.rotationDegrees,
                    maxDimension = targetDimension,
                )
                previewCache[refinedCacheKey] = refinedPreview
                previewImageUri.value = refinedPreview
            } catch (throwable: Throwable) {
                if (throwable !is CancellationException) {
                    previewImageUri.value = currentDisplayUri
                    errorMessage.value = throwable.message ?: "Não foi possível atualizar a prévia do filtro."
                }
            } finally {
                isPreviewLoading.value = false
                isPreviewRefining.value = false
            }
        }
    }

    fun prepareCurrentPagePreview(previewLongSide: Int) {
        val page = uiState.value.currentPage ?: return
        val targetDimension = previewLongSide.coerceIn(1400, 1800)
        previewJob?.cancel()

        if (!page.requiresDerivedImage()) {
            previewImageUri.value = page.canonicalUri
            isPreviewLoading.value = false
            isPreviewRefining.value = false
            return
        }

        val cacheKey = buildPreviewCacheKey(page, page.filterType, targetDimension)
        previewCache[cacheKey]?.let { cachedPreview ->
            previewImageUri.value = cachedPreview
            isPreviewLoading.value = false
            isPreviewRefining.value = false
            return
        }

        isPreviewLoading.value = true
        isPreviewRefining.value = false
        previewJob = viewModelScope.launch {
            errorMessage.value = null
            runCatching {
                val preview = processingRepository.renderPreview(
                    sourceUri = page.sourceUri,
                    filterType = page.filterType,
                    quad = page.quad,
                    rotationDegrees = page.rotationDegrees,
                    maxDimension = targetDimension,
                )
                previewCache[cacheKey] = preview
                previewImageUri.value = preview
            }.onFailure { throwable ->
                if (throwable !is CancellationException) {
                    previewImageUri.value = page.displayUri
                    errorMessage.value = throwable.message ?: "Não foi possível atualizar a prévia da página."
                }
            }
            isPreviewLoading.value = false
        }
    }

    fun applyFilter(filterType: DocumentFilterType) {
        val page = uiState.value.currentPage ?: return
        previewJob?.cancel()
        viewModelScope.launch {
            isProcessing.value = true
            isPreviewLoading.value = false
            isPreviewRefining.value = false
            errorMessage.value = null
            runCatching {
                val effectiveQuad = ensureQuad(page)
                val processedUri = processingRepository.processPage(
                    sourceUri = page.sourceUri,
                    filterType = filterType,
                    quad = effectiveQuad,
                    rotationDegrees = page.rotationDegrees,
                )
                scanRepository.updatePage(
                    scanId = scanId,
                    page = page.copy(
                        quad = effectiveQuad,
                        processedUri = processedUri,
                        filterType = filterType,
                        ocrText = null,
                    ),
                )
                processedUri
            }.onSuccess { processedUri ->
                previewCache.clear()
                previewImageUri.value = processedUri
            }.onFailure { throwable ->
                if (throwable !is CancellationException) {
                    errorMessage.value = throwable.message ?: "Não foi possível aplicar o visual da página."
                }
            }
            isProcessing.value = false
        }
    }

    fun rotateCurrentPage() {
        val page = uiState.value.currentPage ?: return
        previewJob?.cancel()
        viewModelScope.launch {
            isProcessing.value = true
            isPreviewLoading.value = false
            isPreviewRefining.value = false
            errorMessage.value = null
            runCatching {
                val newRotation = (page.rotationDegrees + 90) % 360
                val effectiveQuad = ensureQuad(page)
                val processedUri = processingRepository.processPage(
                    sourceUri = page.sourceUri,
                    filterType = page.filterType,
                    quad = effectiveQuad,
                    rotationDegrees = newRotation,
                )
                scanRepository.updatePage(
                    scanId = scanId,
                    page = page.copy(
                        quad = effectiveQuad,
                        processedUri = processedUri,
                        rotationDegrees = newRotation,
                        ocrText = null,
                    ),
                )
                processedUri
            }.onSuccess { processedUri ->
                previewCache.clear()
                previewImageUri.value = processedUri
            }.onFailure { throwable ->
                if (throwable !is CancellationException) {
                    errorMessage.value = throwable.message ?: "Não foi possível girar a página."
                }
            }
            isProcessing.value = false
        }
    }

    fun renameScan(title: String) {
        val result = validateDocumentNameUseCase(title)
        if (!result.isValid) {
            errorMessage.value = result.errorMessage
            return
        }
        if (result.sanitizedValue == uiState.value.scan?.title) return
        viewModelScope.launch {
            scanRepository.renameScan(scanId, result.sanitizedValue)
        }
    }

    fun updateTags(rawValue: String) {
        val tags = rawValue
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        if (tags == uiState.value.scan?.tags) return
        viewModelScope.launch {
            scanRepository.updateTags(scanId, tags)
        }
    }

    fun movePage(pageId: String, direction: Int) {
        val scan = uiState.value.scan ?: return
        val orderedIds = scan.pages.sortedBy { it.index }.map { it.id }.toMutableList()
        val currentIndex = orderedIds.indexOf(pageId)
        if (currentIndex == -1) return
        val targetIndex = (currentIndex + direction).coerceIn(0, orderedIds.lastIndex)
        if (currentIndex == targetIndex) return
        orderedIds.removeAt(currentIndex)
        orderedIds.add(targetIndex, pageId)
        viewModelScope.launch {
            scanRepository.updatePageOrder(scanId, orderedIds)
        }
    }

    fun deleteCurrentPage() {
        val page = uiState.value.currentPage ?: return
        viewModelScope.launch {
            val outcome = scanRepository.deletePage(scanId, page.id)
            if (outcome.hasCleanupFailures) {
                errorMessage.value = "A página foi excluída, mas um arquivo privado aguarda nova limpeza."
            }
        }
    }

    fun clearMessage() {
        errorMessage.update { null }
    }

    private suspend fun ensureQuad(page: ScanPage): DocumentQuad {
        val existing = page.quad
        if (existing != null) return existing
        val detection = processingRepository.detectDocument(
            imageUri = page.sourceUri,
            profile = uiState.value.scan?.mode.toDocumentProfile(),
        )
        detectionResult.value = detection
        scanRepository.updatePage(
            scanId = scanId,
            page = page.copy(quad = detection.quadOrFullPage()).withInvalidatedDerivedImage(),
        )
        return detection.quadOrFullPage()
    }

    private fun buildPreviewCacheKey(
        page: ScanPage,
        filterType: DocumentFilterType,
        maxDimension: Int,
    ): String = ImagePipelineSpec.buildKey(
        page = page,
        purpose = PageRenderPurpose.PREVIEW,
        filterType = filterType,
        maxDimension = maxDimension,
    ).toString()
}

private fun ScanMode?.toDocumentProfile(): DocumentProfile = when (this) {
    ScanMode.NOTEBOOK -> DocumentProfile.NOTEBOOK
    ScanMode.RECEIPT -> DocumentProfile.RECEIPT
    ScanMode.DOCUMENT, null -> DocumentProfile.GENERAL
}
