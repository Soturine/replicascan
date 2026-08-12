package com.soturine.scanora.feature.camera

import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.soturine.scanora.core.common.model.ScanMode

class CameraCaptureViewModel(
    mode: ScanMode,
) : ViewModel() {
    private val captureInFlight = AtomicBoolean(false)
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

    fun onError(message: String) {
        captureInFlight.set(false)
        _uiState.update { it.copy(isCapturing = false, errorMessage = message) }
    }
}

