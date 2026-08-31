package com.soturine.replicascan.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.DeletionOutcome
import com.soturine.replicascan.core.common.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanDetailViewModel(
    private val scanId: String,
    private val scanRepository: ScanRepository,
) : ViewModel() {
    val scan: StateFlow<ScanDocument?> = scanRepository.observeScan(scanId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun toggleFavorite() {
        viewModelScope.launch {
            scanRepository.toggleFavorite(scanId)
        }
    }

    fun deleteScan(onDeleted: (DeletionOutcome) -> Unit) {
        viewModelScope.launch {
            onDeleted(scanRepository.deleteScan(scanId))
        }
    }
}

