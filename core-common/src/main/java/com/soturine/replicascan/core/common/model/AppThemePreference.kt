package com.soturine.replicascan.core.common.model

enum class AppThemePreference(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(value: String): AppThemePreference =
            entries.firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

