package com.soturine.scanora.feature.camera

import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.common.model.ScanMode
import org.junit.Test

class CameraCaptureViewModelTest {
    @Test
    fun captureGateAllowsOnlyOneRequestUntilCompletion() {
        val viewModel = CameraCaptureViewModel(ScanMode.DOCUMENT)

        assertThat(viewModel.tryStartCapture()).isTrue()
        assertThat(viewModel.tryStartCapture()).isFalse()
        assertThat(viewModel.uiState.value.isCapturing).isTrue()

        viewModel.onCaptureFinished()

        assertThat(viewModel.tryStartCapture()).isTrue()
    }

    @Test
    fun errorReleasesCaptureGate() {
        val viewModel = CameraCaptureViewModel(ScanMode.DOCUMENT)
        viewModel.tryStartCapture()

        viewModel.onError("falha")

        assertThat(viewModel.uiState.value.isCapturing).isFalse()
        assertThat(viewModel.tryStartCapture()).isTrue()
    }
}
