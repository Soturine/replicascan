package com.soturine.replicascan.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.AppThemePreference
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanSecondaryButton
import com.soturine.replicascan.core.ui.localizedTitle

private enum class Picker { THEME, LANGUAGE, PDF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    currentLanguageTags: String,
    onThemeSelected: (AppThemePreference) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onPdfQualitySelected: (PdfQuality) -> Unit,
    onOpenIntroduction: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }
    val languageTag = AppLanguages.resolve(currentLanguageTags)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            ReplicaScanContent {
                Group(stringResource(R.string.settings_appearance_section)) {
                    PreferenceRow(Icons.Outlined.Palette, stringResource(R.string.settings_theme), state.preferences.themePreference.label()) {
                        picker = Picker.THEME
                    }
                    PreferenceRow(
                        Icons.Outlined.Language,
                        stringResource(R.string.settings_language),
                        AppLanguages.autonym(languageTag) ?: stringResource(R.string.settings_language_system),
                    ) { picker = Picker.LANGUAGE }
                }
                Group(stringResource(R.string.settings_export_section)) {
                    PreferenceRow(
                        Icons.Outlined.PictureAsPdf,
                        stringResource(R.string.settings_pdf_quality),
                        state.preferences.defaultPdfQuality.localizedTitle(),
                    ) { picker = Picker.PDF }
                }
                Group(stringResource(R.string.settings_help_section)) {
                    PreferenceRow(Icons.Outlined.School, stringResource(R.string.settings_introduction), null, onClick = onOpenIntroduction)
                    PreferenceRow(Icons.Outlined.PrivacyTip, stringResource(R.string.settings_privacy), null, external = true, onClick = onOpenPrivacyPolicy)
                    PreferenceRow(Icons.Outlined.Info, stringResource(R.string.settings_about), null, onClick = onOpenAbout)
                }
            }
        }
    }
    when (picker) {
        Picker.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            choices = AppThemePreference.entries.map { it to it.label() },
            selected = state.preferences.themePreference,
            onSelect = { onThemeSelected(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.settings_language),
            choices = listOf(AppLanguages.SYSTEM_TAG to stringResource(R.string.settings_language_system)) +
                AppLanguages.all.map { it.tag to it.autonym },
            selected = languageTag,
            onSelect = { onLanguageSelected(it); picker = null },
            onDismiss = { picker = null },
        )
        Picker.PDF -> ChoiceDialog(
            title = stringResource(R.string.settings_pdf_quality),
            choices = PdfQuality.entries.map { it to it.localizedTitle() },
            selected = state.preferences.defaultPdfQuality,
            onSelect = { onPdfQualitySelected(it); picker = null },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
        )
        content()
    }
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    title: String,
    value: String?,
    external: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.selectable(selected = false, role = Role.Button, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        headlineContent = { Text(title) },
        supportingContent = value?.let { { Text(it) } },
        trailingContent = if (external) {
            { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).selectableGroup()) {
                items(choices, key = { it.first.toString() }) { (choice, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = choice == selected, role = Role.RadioButton, onClick = { onSelect(choice) })
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onOpenPrivacyPolicy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_about)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReplicaScanMascot(ReplicaScanMascotState.Welcome, size = 144.dp)
            Text("ReplicaScan", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.settings_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            ReplicaScanSecondaryButton(
                text = stringResource(R.string.settings_privacy),
                onClick = onOpenPrivacyPolicy,
                outlined = true,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                },
            )
        }
    }
}

@Composable
private fun AppThemePreference.label(): String = stringResource(
    when (this) {
        AppThemePreference.SYSTEM -> R.string.settings_theme_system
        AppThemePreference.LIGHT -> R.string.settings_theme_light
        AppThemePreference.DARK -> R.string.settings_theme_dark
    },
)
