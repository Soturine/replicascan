package com.soturine.scanora.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.scanora.core.common.model.DocumentDetectionConfidence
import com.soturine.scanora.core.common.model.DocumentProfile
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PointValue
import com.soturine.scanora.core.common.repository.DocumentProcessingRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.soturine.scanora.core.common.model.ScanMode

class CameraCaptureViewModel(
    mode: ScanMode,
    private val processingRepository: DocumentProcessingRepository? = null,
) : ViewModel() {
    private val captureInFlight = AtomicBoolean(false)
    private val analysisInFlight = AtomicBoolean(false)
    private val lastAnalysisAt = AtomicLong(0L)
    private var missedFrames = 0
    private val _uiState = MutableStateFlow(CameraCaptureUiState(mode = mode))
    val uiState: StateFlow<CameraCaptureUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
    }

    fun tryStartCapture(): Boolean {
        if (!captureInFlight.compareAndSet(false, true)) return false
        _uiState.update { it.copy(isCapturing = true, errorMessage = null) }
        return true
    }

    fun onCaptureFinished() {
        captureInFlight.set(false)
        _uiState.update { it.copy(isCapturing = false) }
    }

    fun onCaptured(uri: String) {
        captureInFlight.set(false)
        _uiState.update { state ->
            state.copy(
                isCapturing = false,
                capturedUris = state.capturedUris + uri,
                errorMessage = null,
            )
        }
    }

    fun analyzeFrame(luma: IntArray, width: Int, height: Int) {
        val repository = processingRepository ?: return
        val now = System.currentTimeMillis()
        if (now - lastAnalysisAt.get() < ANALYSIS_INTERVAL_MILLIS) return
        if (!analysisInFlight.compareAndSet(false, true)) return
        lastAnalysisAt.set(now)
        viewModelScope.launch {
            try {
                val detection = repository.detectPreviewLuma(
                    luma = luma,
                    width = width,
                    height = height,
                    profile = DocumentProfile.GENERAL,
                )
                val detected = detection.quad
                if (detected == null || detection.confidence == DocumentDetectionConfidence.NONE) {
                    missedFrames++
                    if (missedFrames >= MISSES_BEFORE_CLEAR) {
                        _uiState.update { it.copy(liveQuad = null, liveConfidence = DocumentDetectionConfidence.NONE) }
                    }
                } else {
                    missedFrames = 0
                    _uiState.update { state ->
                        state.copy(
                            liveQuad = state.liveQuad?.blend(detected, LIVE_SMOOTHING) ?: detected,
                            liveConfidence = detection.confidence,
                        )
                    }
                }
            } finally {
                analysisInFlight.set(false)
            }
        }
    }

    fun onError(message: String) {
        captureInFlight.set(false)
        _uiState.update { it.copy(isCapturing = false, errorMessage = message) }
    }

    private fun DocumentQuad.blend(next: DocumentQuad, weight: Float): DocumentQuad {
        fun PointValue.mix(other: PointValue) = PointValue(
            x = x * (1f - weight) + other.x * weight,
            y = y * (1f - weight) + other.y * weight,
        )
        return DocumentQuad(
            topLeft = topLeft.mix(next.topLeft),
            topRight = topRight.mix(next.topRight),
            bottomRight = bottomRight.mix(next.bottomRight),
            bottomLeft = bottomLeft.mix(next.bottomLeft),
        )
    }

    private companion object {
        const val ANALYSIS_INTERVAL_MILLIS = 260L
        const val MISSES_BEFORE_CLEAR = 3
        const val LIVE_SMOOTHING = 0.34f
    }
}

