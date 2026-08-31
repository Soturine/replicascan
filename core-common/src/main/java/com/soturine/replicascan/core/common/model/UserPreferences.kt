package com.soturine.replicascan.core.common.model

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    val defaultScanMode: ScanMode = ScanMode.DOCUMENT,
    val defaultPdfQuality: PdfQuality = PdfQuality.BALANCED,
)

