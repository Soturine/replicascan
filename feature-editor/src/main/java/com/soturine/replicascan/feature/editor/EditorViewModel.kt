package com.soturine.replicascan.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.ImagePipelineSpec
import com.soturine.replicascan.core.common.model.PageRenderPurpose
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.isUnmodified
import com.soturine.replicascan.core.common.model.requiresDerivedImage
import com.soturine.replicascan.core.common.model.withInvalidatedDerivedImage
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.ScanRepository
import com.soturine.replicascan.core.common.result.NameValidationError
import com.soturine.replicascan.core.common.usecase.ValidateDocumentNameUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(
    private val scanId: String,
    initialPageId: String?,
    private val scanRepository: ScanRepository,
    private val processingRepository: DocumentProcessingRepository,
    private val validateDocumentNameUseCase: ValidateDocumentNameUseCase = ValidateDocumentNameUseCase(),
) : ViewModel() {
    private val selectedPageId = MutableStateFlow(initialPageId)
    private val work = MutableStateFlow(EditorWork())
    private val previewImageUri = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<EditorMessage?>(null)

    private val previewCache = mutableMapOf<String, String>()
    private var previewJob: Job? = null

    val uiState: StateFlow<EditorUiState> = combine(
        scanRepository.observeScan(scanId),
        selectedPageId,
        work,
        previewImageUri,
        message,
    ) { scan, pageId, currentWork, previewUri, currentMessage ->
        val orderedPages = scan?.pages?.sortedBy { it.index }.orEmpty()
        val page = orderedPages.firstOrNull { it.id == pageId } ?: orderedPages.firstOrNull()
        EditorUiState(
            isLoaded = true,
            scan = scan,
            currentPage = page,
            isProcessing = currentWork.processing,
            isPreviewLoading = currentWork.previewLoading,
            previewImageUri = previewUri ?: page?.displayUri,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditorUiState(),
    )

    fun selectPage(pageId: String) {
        if (pageId == selectedPageId.value) return
        previewJob?.cancel()
        selectedPageId.value = pageId
        previewImageUri.value = null
        work.value = work.value.copy(previewLoading = false)
    }

    fun updateQuad(quad: DocumentQuad, onSaved: () -> Unit) {
        val page = uiState.value.currentPage ?: return
        val normalized = quad.takeUnless { it == DocumentQuad.FULL_PAGE }
        persistPage(page, page.copy(quad = normalized), onSaved)
    }

    fun applyFilter(filterType: DocumentFilterType, onApplied: () -> Unit) {
        val page = uiState.value.currentPage ?: return
        if (page.filterType == filterType) {
            onApplied()
            return
        }
        persistPage(page, page.copy(filterType = filterType), onApplied)
    }

    fun rotateCurrentPage() {
        val page = uiState.value.currentPage ?: return
        persistPage(page, page.copy(rotationDegrees = ImagePipelineSpec.normalizeRotation(page.rotationDegrees + 90)))
    }

    /** Renders a downsampled look preview; never touches the persisted page. */
    fun prepareFilterPreview(filterType: DocumentFilterType, previewLongSide: Int) {
        val page = uiState.value.currentPage ?: return
        renderPreview(page, filterType, previewLongSide)
    }

    fun prepareCurrentPagePreview(previewLongSide: Int) {
        val page = uiState.value.currentPage ?: return
        if (!page.requiresDerivedImage()) {
            previewJob?.cancel()
            previewImageUri.value = page.canonicalUri
            work.value = work.value.copy(previewLoading = false)
            return
        }
        renderPreview(page, page.filterType, previewLongSide)
    }

    fun renameScan(title: String): Boolean {
        val result = validateDocumentNameUseCase(title)
        if (!result.isValid) {
            message.value = when (result.error) {
                NameValidationError.TOO_LONG -> EditorMessage.NAME_TOO_LONG
                else -> EditorMessage.NAME_REQUIRED
            }
            return false
        }
        if (result.sanitizedValue != uiState.value.scan?.title) {
            viewModelScope.launch { scanRepository.renameScan(scanId, result.sanitizedValue) }
        }
        return true
    }

    fun updateTags(rawValue: String) {
        val tags = rawValue.split(",").map(String::trim).filter(String::isNotBlank).distinct()
        if (tags == uiState.value.scan?.tags) return
        viewModelScope.launch { scanRepository.updateTags(scanId, tags) }
    }

    /** [direction] is -1 for “earlier in the document” and +1 for “later”; layout direction does not matter. */
    fun movePage(pageId: String, direction: Int) {
        val scan = uiState.value.scan ?: return
        val orderedIds = scan.pages.sortedBy { it.index }.map { it.id }.toMutableList()
        val currentIndex = orderedIds.indexOf(pageId)
        if (currentIndex == -1) return
        val targetIndex = (currentIndex + direction).coerceIn(0, orderedIds.lastIndex)
        if (currentIndex == targetIndex) return
        orderedIds.add(targetIndex, orderedIds.removeAt(currentIndex))
        viewModelScope.launch {
            runCatching { scanRepository.updatePageOrder(scanId, orderedIds) }
                .onFailure { if (it !is CancellationException) message.value = EditorMessage.SAVE_FAILED }
        }
    }

    fun deleteCurrentPage() {
        val page = uiState.value.currentPage ?: return
        previewJob?.cancel()
        previewImageUri.value = null
        viewModelScope.launch {
            val outcome = scanRepository.deletePage(scanId, page.id)
            selectedPageId.value = null
            if (outcome.hasCleanupFailures) message.value = EditorMessage.CLEANUP_PENDING
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun renderPreview(page: ScanPage, filterType: DocumentFilterType, previewLongSide: Int) {
        previewJob?.cancel()
        val maxDimension = previewLongSide.coerceIn(MIN_PREVIEW_SIDE, MAX_PREVIEW_SIDE)
        val cacheKey = ImagePipelineSpec.buildKey(page, PageRenderPurpose.PREVIEW, filterType, maxDimension).toString()
        previewCache[cacheKey]?.let { cached ->
            previewImageUri.value = cached
            work.value = work.value.copy(previewLoading = false)
            return
        }
        work.value = work.value.copy(previewLoading = true)
        previewJob = viewModelScope.launch {
            try {
                val preview = processingRepository.renderPreview(
                    sourceUri = page.sourceUri,
                    filterType = filterType,
                    quad = page.quad,
                    rotationDegrees = page.rotationDegrees,
                    maxDimension = maxDimension,
                )
                previewCache[cacheKey] = preview
                previewImageUri.value = preview
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                previewImageUri.value = page.displayUri
                message.value = EditorMessage.PREVIEW_FAILED
            } finally {
                work.value = work.value.copy(previewLoading = false)
            }
        }
    }

    /**
     * Saves the page's logical state and refreshes its derived thumbnail. The canonical source is
     * never rewritten; a failed render leaves the previous state in place. Recognized text is kept
     * for look changes because OCR reads geometry only.
     */
    private fun persistPage(previous: ScanPage, updated: ScanPage, onSaved: (() -> Unit)? = null) {
        if (work.value.processing) return
        previewJob?.cancel()
        previewImageUri.value = null
        work.value = work.value.copy(processing = true, previewLoading = false)
        viewModelScope.launch {
            try {
                val derivedUri = if (updated.isUnmodified()) {
                    null
                } else {
                    processingRepository.processPage(updated.sourceUri, updated.filterType, updated.quad, updated.rotationDegrees)
                }
                val geometryChanged = previous.quad != updated.quad || previous.rotationDegrees != updated.rotationDegrees
                scanRepository.updatePage(
                    scanId,
                    updated.withInvalidatedDerivedImage(clearOcr = geometryChanged).copy(processedUri = derivedUri),
                )
                onSaved?.invoke()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                message.value = EditorMessage.SAVE_FAILED
            } finally {
                work.value = work.value.copy(processing = false)
            }
        }
    }

    private data class EditorWork(
        val processing: Boolean = false,
        val previewLoading: Boolean = false,
    )

    private companion object {
        const val MIN_PREVIEW_SIDE = 1_200
        const val MAX_PREVIEW_SIDE = 1_800
    }
}

enum class EditorMessage {
    PREVIEW_FAILED,
    SAVE_FAILED,
    CLEANUP_PENDING,
    NAME_REQUIRED,
    NAME_TOO_LONG,
}

data class EditorUiState(
    val isLoaded: Boolean = false,
    val scan: ScanDocument? = null,
    val currentPage: ScanPage? = null,
    val isProcessing: Boolean = false,
    val isPreviewLoading: Boolean = false,
    val previewImageUri: String? = null,
    val message: EditorMessage? = null,
)
