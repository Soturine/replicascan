package com.soturine.replicascan.feature.settings

import com.soturine.replicascan.core.common.model.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
)

