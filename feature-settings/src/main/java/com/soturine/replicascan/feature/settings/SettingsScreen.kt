package com.soturine.replicascan.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.soturine.replicascan.core.common.model.*
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import com.soturine.replicascan.core.ui.component.SectionHeader
import com.soturine.replicascan.core.ui.localizedTitle

private enum class Picker { THEME, LANGUAGE, PDF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (AppThemePreference) -> Unit,
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
            SectionHeader(
                eyebrow = stringResource(R.string.settings_eyebrow),
                title = stringResource(R.string.settings_heading),
                supportingText = stringResource(R.string.settings_supporting),
            )
            Group(stringResource(R.string.settings_theme_section)) {
                PreferenceRow(Icons.Outlined.Palette, stringResource(R.string.settings_theme_section), state.preferences.themePreference.label()) { picker = Picker.THEME }
                PreferenceRow(Icons.Outlined.Language, stringResource(R.string.settings_language_section), languageLabel(currentLanguageTag)) { picker = Picker.LANGUAGE }
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
        Picker.LANGUAGE -> LanguagePickerDialog(
            stringResource(R.string.settings_language_section),
            currentLanguageTag,
            { onLanguageSelected(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.PDF -> ChoiceDialog(stringResource(R.string.settings_pdf_quality_section), PdfQuality.entries.map { it to it.localizedTitle() }, state.preferences.defaultPdfQuality, { onPdfQualitySelected(it); picker = null }) { picker = null }
        null -> Unit
    }
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val systemLabel = stringResource(R.string.settings_language_system)
    val options = remember(systemLabel) {
        listOf(
            "" to systemLabel,
            "pt-BR" to "Português (Brasil)",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "it" to "Italiano",
            "ar" to "العربية",
            "de" to "Deutsch",
            "id" to "Bahasa Indonesia",
            "hi" to "हिन्दी",
            "tr" to "Türkçe",
            "ja" to "日本語",
            "ko" to "한국어",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(options, key = { it.first.ifEmpty { "system" } }) { (tag, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(
                            selected = tag == selected,
                            onClick = { onSelect(tag) },
                        ).padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == selected, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable private fun Group(title: String, content: @Composable () -> Unit) = Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        tonalElevation = 1.dp,
    ) {
        Column { content() }
    }
}

@Composable private fun PreferenceRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(9.dp).size(22.dp))
        }
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
            RadioButton(selected = choice == selected, onClick = { onSelect(choice) }); Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        }
    } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AboutScreen(onOpenPrivacyPolicy: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_about_title)) }) }) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            ReplicaScanMascot(ReplicaScanMascotState.Welcome, size = 180.dp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Text(
                    stringResource(R.string.settings_about_body),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReplicaScanPrimaryButton(
                text = stringResource(R.string.settings_open_privacy),
                onClick = onOpenPrivacyPolicy,
                icon = { Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)) },
            )
        }
    }
}

@Composable private fun AppThemePreference.label(): String = stringResource(when (this) {
    AppThemePreference.SYSTEM -> R.string.settings_theme_system; AppThemePreference.LIGHT -> R.string.settings_theme_light; AppThemePreference.DARK -> R.string.settings_theme_dark
})

@Composable private fun languageLabel(tag: String): String = stringResource(when (tag) {
    "pt-BR" -> R.string.settings_language_portuguese
    "en" -> R.string.settings_language_english
    "es" -> R.string.settings_language_spanish
    "fr" -> R.string.settings_language_french
    "it" -> R.string.settings_language_italian
    "ar" -> R.string.settings_language_arabic
    "de" -> R.string.settings_language_german
    "id" -> R.string.settings_language_indonesian
    "hi" -> R.string.settings_language_hindi
    "tr" -> R.string.settings_language_turkish
    "ja" -> R.string.settings_language_japanese
    "ko" -> R.string.settings_language_korean
    else -> R.string.settings_language_system
})
