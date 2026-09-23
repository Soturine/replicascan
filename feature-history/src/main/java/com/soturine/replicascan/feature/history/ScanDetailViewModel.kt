package com.soturine.replicascan.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.DeletionOutcome
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.repository.ScanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanDetailViewModel(
    private val scanId: String,
    private val scanRepository: ScanRepository,
) : ViewModel() {
    /** `null` until Room answers; then [DetailState.scan] is `null` only if the document is gone. */
    val state: StateFlow<DetailState?> = scanRepository.observeScan(scanId)
        .map { DetailState(it) }
        .stateIn(
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

data class DetailState(val scan: ScanDocument?)
