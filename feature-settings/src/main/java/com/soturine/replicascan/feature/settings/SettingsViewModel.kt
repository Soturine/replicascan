package com.soturine.replicascan.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.AppThemePreference
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanMode
import com.soturine.replicascan.core.common.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = preferencesRepository.preferences
        .map { SettingsUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setTheme(preference: AppThemePreference) {
        viewModelScope.launch {
            preferencesRepository.setThemePreference(preference)
        }
    }

    fun setDefaultMode(mode: ScanMode) {
        viewModelScope.launch {
            preferencesRepository.setDefaultScanMode(mode)
        }
    }

    fun setPdfQuality(quality: PdfQuality) {
        viewModelScope.launch {
            preferencesRepository.setDefaultPdfQuality(quality)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }
}

