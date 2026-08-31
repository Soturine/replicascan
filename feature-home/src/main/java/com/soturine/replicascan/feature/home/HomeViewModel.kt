package com.soturine.replicascan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.repository.ScanRepository
import com.soturine.replicascan.core.common.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    scanRepository: ScanRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        scanRepository.observeRecentScans(limit = 5),
        preferencesRepository.preferences,
    ) { scans, preferences ->
        HomeUiState(
            isLoading = false,
            manualMode = preferences.defaultScanMode,
            recentScans = scans,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

