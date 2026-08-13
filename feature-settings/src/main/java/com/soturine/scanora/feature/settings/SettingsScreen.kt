package com.soturine.scanora.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soturine.scanora.core.common.model.*
import com.soturine.scanora.core.ui.component.ScanoraMascot
import com.soturine.scanora.core.ui.component.ScanoraMascotState
import com.soturine.scanora.core.ui.localizedTitle

private enum class Picker { THEME, LANGUAGE, MODE, PDF }

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
    var picker by remember { mutableStateOf<Picker?>(null) }
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Group(stringResource(R.string.settings_theme_section)) {
                PreferenceRow(Icons.Outlined.Palette, stringResource(R.string.settings_theme_section), state.preferences.themePreference.label()) { picker = Picker.THEME }
                PreferenceRow(Icons.Outlined.Language, stringResource(R.string.settings_language_section), languageLabel(currentLanguageTag)) { picker = Picker.LANGUAGE }
            }
            Group(stringResource(R.string.settings_default_mode_section)) {
                PreferenceRow(Icons.Outlined.PhotoCamera, stringResource(R.string.settings_default_mode_section), state.preferences.defaultScanMode.localizedTitle()) { picker = Picker.MODE }
            }
            Group(stringResource(R.string.settings_pdf_quality_section)) {
                PreferenceRow(Icons.Outlined.PictureAsPdf, stringResource(R.string.settings_pdf_quality_section), state.preferences.defaultPdfQuality.localizedTitle()) { picker = Picker.PDF }
            }
            Group(stringResource(R.string.settings_open_about)) {
                PreferenceRow(Icons.Outlined.RestartAlt, stringResource(R.string.settings_reset_onboarding), null, onResetOnboarding)
                PreferenceRow(Icons.Outlined.Info, stringResource(R.string.settings_open_about), stringResource(R.string.settings_version), onOpenAbout)
            }
        }
    }
    when (picker) {
        Picker.THEME -> ChoiceDialog(stringResource(R.string.settings_theme_section), AppThemePreference.entries.map { it to it.label() }, state.preferences.themePreference, { onThemeSelected(it); picker = null }) { picker = null }
        Picker.LANGUAGE -> ChoiceDialog(stringResource(R.string.settings_language_section), listOf("", "pt-BR", "en", "es", "fr", "it").map { it to languageLabel(it) }, currentLanguageTag, { onLanguageSelected(it); picker = null }) { picker = null }
        Picker.MODE -> ChoiceDialog(stringResource(R.string.settings_default_mode_section), ScanMode.entries.map { it to it.localizedTitle() }, state.preferences.defaultScanMode, { onDefaultModeSelected(it); picker = null }) { picker = null }
        Picker.PDF -> ChoiceDialog(stringResource(R.string.settings_pdf_quality_section), PdfQuality.entries.map { it to it.localizedTitle() }, state.preferences.defaultPdfQuality, { onPdfQualitySelected(it); picker = null }) { picker = null }
        null -> Unit
    }
}

@Composable private fun Group(title: String, content: @Composable () -> Unit) = Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
    content()
}

@Composable private fun PreferenceRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable private fun <T> ChoiceDialog(title: String, choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column { choices.forEach { (choice, label) ->
        Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = choice == selected, onClick = { onSelect(choice) }); Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
        }
    } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AboutScreen(onOpenPrivacyPolicy: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_about_title)) }) }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ScanoraMascot(ScanoraMascotState.Welcome, size = 180.dp)
            Text(stringResource(R.string.settings_about_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_open_privacy)) }
        }
    }
}

@Composable private fun AppThemePreference.label(): String = stringResource(when (this) {
    AppThemePreference.SYSTEM -> R.string.settings_theme_system; AppThemePreference.LIGHT -> R.string.settings_theme_light; AppThemePreference.DARK -> R.string.settings_theme_dark
})

@Composable private fun languageLabel(tag: String): String = stringResource(when (tag) {
    "pt-BR" -> R.string.settings_language_portuguese; "en" -> R.string.settings_language_english; "es" -> R.string.settings_language_spanish; "fr" -> R.string.settings_language_french; "it" -> R.string.settings_language_italian; else -> R.string.settings_language_system
})
