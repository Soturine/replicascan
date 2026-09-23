package com.soturine.replicascan.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    scanRepository: ScanRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = scanRepository.observeRecentScans(limit = RECENT_LIMIT)
        .map { scans -> HomeUiState(isLoading = false, recentScans = scans) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    private companion object {
        const val RECENT_LIMIT = 5
    }
}
