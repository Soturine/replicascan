package com.soturine.scanora.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.scanora.core.common.repository.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    scanRepository: ScanRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = query.flatMapLatest { currentQuery ->
        scanRepository.observeScans(currentQuery).map { scans ->
            HistoryUiState(query = currentQuery, scans = scans)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }
}

