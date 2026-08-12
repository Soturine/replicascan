package com.soturine.scanora.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soturine.scanora.core.common.model.AppThemePreference
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.ui.component.OptionCard
import com.soturine.scanora.core.ui.component.SectionHeader
import com.soturine.scanora.core.ui.localizedDescription
import com.soturine.scanora.core.ui.localizedTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (AppThemePreference) -> Unit,
    onDefaultModeSelected: (ScanMode) -> Unit,
    onPdfQualitySelected: (PdfQuality) -> Unit,
    onResetOnboarding: () -> Unit,
    onOpenAbout: () -> Unit,
    currentLanguageTag: String = "",
    onLanguageSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionHeader(
                eyebrow = stringResource(id = R.string.settings_eyebrow),
                title = stringResource(id = R.string.settings_heading),
                supportingText = stringResource(id = R.string.settings_supporting),
            )

            SettingsSection(
                title = stringResource(id = R.string.settings_theme_section),
                supportingText = stringResource(id = R.string.settings_theme_supporting),
            ) {
                AppThemePreference.entries.forEach { preference ->
                    OptionCard(
                        title = preference.displayName(),
                        subtitle = preference.description(),
                        selected = state.preferences.themePreference == preference,
                        onClick = { onThemeSelected(preference) },
                    )
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_language_section),
                supportingText = stringResource(R.string.settings_language_supporting),
            ) {
                OptionCard(
                    title = languageLabel(currentLanguageTag),
                    subtitle = stringResource(R.string.settings_language_supporting),
                    selected = false,
                    onClick = { showLanguageDialog = true },
                )
            }

            SettingsSection(
                title = stringResource(id = R.string.settings_default_mode_section),
                supportingText = stringResource(id = R.string.settings_mode_supporting),
            ) {
                ScanMode.entries.forEach { mode ->
                    OptionCard(
                        title = mode.localizedTitle(),
                        subtitle = mode.localizedDescription(),
                        selected = state.preferences.defaultScanMode == mode,
                        onClick = { onDefaultModeSelected(mode) },
                    )
                }
            }

            SettingsSection(
                title = stringResource(id = R.string.settings_pdf_quality_section),
                supportingText = stringResource(id = R.string.settings_pdf_supporting),
            ) {
                PdfQuality.entries.forEach { quality ->
                    OptionCard(
                        title = quality.localizedTitle(),
                        subtitle = quality.localizedDescription(),
                        selected = state.preferences.defaultPdfQuality == quality,
                        onClick = { onPdfQualitySelected(quality) },
                    )
                }
            }

            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetOnboarding,
            ) {
                Text(text = stringResource(id = R.string.settings_reset_onboarding))
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenAbout,
            ) {
                Text(text = stringResource(id = R.string.settings_open_about))
            }
        }
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language_section)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("", "pt-BR", "en", "es", "fr", "it").forEach { tag ->
                        OptionCard(
                            title = languageLabel(tag),
                            selected = currentLanguageTag == tag,
                            onClick = { onLanguageSelected(tag); showLanguageDialog = false },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_about_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_about_body),
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenPrivacyPolicy,
            ) {
                Text(text = stringResource(id = R.string.settings_open_privacy))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    supportingText: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun AppThemePreference.displayName(): String = stringResource(when (this) {
    AppThemePreference.SYSTEM -> R.string.settings_theme_system
    AppThemePreference.LIGHT -> R.string.settings_theme_light
    AppThemePreference.DARK -> R.string.settings_theme_dark
})

@Composable
private fun AppThemePreference.description(): String = stringResource(when (this) {
    AppThemePreference.SYSTEM -> R.string.settings_theme_system_description
    AppThemePreference.LIGHT -> R.string.settings_theme_light_description
    AppThemePreference.DARK -> R.string.settings_theme_dark_description
})

@Composable
private fun languageLabel(tag: String): String = stringResource(when (tag) {
    "pt-BR" -> R.string.settings_language_portuguese
    "en" -> R.string.settings_language_english
    "es" -> R.string.settings_language_spanish
    "fr" -> R.string.settings_language_french
    "it" -> R.string.settings_language_italian
    else -> R.string.settings_language_system
})
