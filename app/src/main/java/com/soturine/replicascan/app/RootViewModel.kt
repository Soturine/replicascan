package com.soturine.replicascan.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soturine.replicascan.core.common.model.AppThemePreference
import com.soturine.replicascan.core.common.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RootUiState(
    val isReady: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val themePreference: AppThemePreference = AppThemePreference.SYSTEM,
)

class RootViewModel(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<RootUiState> = preferencesRepository.preferences
        .map { preferences ->
            RootUiState(
                isReady = true,
                onboardingCompleted = preferences.onboardingCompleted,
                themePreference = preferences.themePreference,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RootUiState(),
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }
}

