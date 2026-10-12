package com.soturine.replicascan.feature.export

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import com.soturine.replicascan.core.ui.component.AsyncUriImage
import com.soturine.replicascan.core.common.usecase.ValidateDocumentNameUseCase
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.ui.component.EmptyStateCard
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import com.soturine.replicascan.core.ui.component.ReplicaScanSecondaryButton
import com.soturine.replicascan.core.ui.localizedDescription
import com.soturine.replicascan.core.ui.localizedTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    state: ExportUiState,
    onSelectFormat: (ExportFormat) -> Unit,
    onSelectQuality: (PdfQuality) -> Unit,
    onSelectPageSize: (PdfPageSize) -> Unit,
    onExport: (String) -> Unit,
    onShare: (List<ExportedFile>) -> Unit,
    onOpenFile: (ExportedFile) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var fileName by rememberSaveable(state.scan?.id, state.scan?.title) { mutableStateOf(state.scan?.title.orEmpty()) }
    val nameValidation = remember(fileName) { ValidateDocumentNameUseCase()(fileName) }
    BackHandler(enabled = state.isExporting) { /* Keep the owning destination until the atomic write finishes. */ }
    val messageText = state.message?.let { message ->
        when (message) {
            ExportMessage.EMPTY_DOCUMENT -> stringResource(R.string.export_message_empty)
            ExportMessage.PAGE_UNREADABLE -> state.failedPageNumber
                ?.let { stringResource(R.string.export_message_page_unreadable, it) }
                ?: stringResource(R.string.export_message_failed)
            ExportMessage.WRITE_FAILED -> stringResource(R.string.export_message_failed)
            ExportMessage.LIBRARY_UPDATE_FAILED -> stringResource(R.string.export_library_update_failed)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isExporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.export_back))
                    }
                },
            )
        },
        bottomBar = {
            val scan = state.scan
            if (scan != null && state.exportedFiles.isEmpty() && !state.isExporting) {
                Surface {
                    Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                        ReplicaScanPrimaryButton(
                            text = stringResource(R.string.export_action),
                            onClick = { onExport(nameValidation.sanitizedValue) },
                            enabled = nameValidation.isValid && scan.pages.isNotEmpty(),
                            icon = {
                                Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val scan = state.scan
        if (scan == null) {
            EmptyStateCard(
                title = stringResource(R.string.export_missing_title),
                message = stringResource(R.string.export_missing_message),
                mascotState = ReplicaScanMascotState.Attention,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
            return@Scaffold
        }
        AnimatedContent(
            targetState = if (state.exportedFiles.isNotEmpty()) 2 else if (state.isExporting) 1 else 0,
            label = "exportState",
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) { done ->
            if (done == 2) {
                ExportDone(state.exportedFiles, warning = messageText, onOpenFile = onOpenFile, onShare = onShare)
            } else if (done == 1) {
                ExportProgress(scan)
            } else {
                ExportOptions(
                    state = state,
                    fileName = fileName,
                    onNameChange = { fileName = it; onClearMessage() },
                    nameValid = nameValidation.isValid,
                    errorText = messageText,
                    pageCount = scan.pageCount,
                    onSelectFormat = onSelectFormat,
                    onSelectQuality = onSelectQuality,
                    onSelectPageSize = onSelectPageSize,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ExportOptions(
    state: ExportUiState,
    fileName: String,
    onNameChange: (String) -> Unit,
    nameValid: Boolean,
    errorText: String?,
    pageCount: Int,
    onSelectFormat: (ExportFormat) -> Unit,
    onSelectQuality: (PdfQuality) -> Unit,
    onSelectPageSize: (PdfPageSize) -> Unit,
) {
    var moreOptions by rememberSaveable { mutableStateOf(false) }
    val isPdf = state.selectedFormat == ExportFormat.PDF
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        ReplicaScanContent {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AsyncUriImage(state.scan?.coverPage?.displayUri, fallbackImageUri = state.scan?.coverPage?.sourceUri,
                        modifier = Modifier.size(54.dp, 72.dp), contentScale = ContentScale.Fit, maxDimension = 240)
                    Text(state.scan?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
            }
            if (errorText != null) {
                EmptyStateCard(stringResource(R.string.export_recovery), errorText, mascotState = ReplicaScanMascotState.Attention)
            }
            OutlinedTextField(value = fileName, onValueChange = onNameChange, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.export_filename)) }, singleLine = true, isError = !nameValid,
                supportingText = if (!nameValid) ({ Text(stringResource(R.string.export_filename_invalid)) }) else null)
            Text(
                text = pluralStringResource(R.plurals.export_summary, pageCount, pageCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Section(stringResource(R.string.export_format_section)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(selected = state.selectedFormat == format, onClick = { onSelectFormat(format) },
                            label = { Text(format.title, style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.heightIn(min = 56.dp).testTag("format-${format.name}"))
                    }
                }
            }
            if (isPdf) {
                Section(stringResource(R.string.export_quality_section)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = state.selectedQuality == quality,
                                onClick = { onSelectQuality(quality) },
                                label = { Text(quality.localizedTitle()) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                    Text(
                        text = state.selectedQuality.localizedDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { moreOptions = !moreOptions }) {
                    Text(stringResource(R.string.export_more_options))
                    Spacer(Modifier.width(4.dp))
                    Icon(if (moreOptions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
                AnimatedVisibility(visible = moreOptions) {
                    Section(stringResource(R.string.export_page_size_section)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PdfPageSize.entries.forEach { size ->
                                FilterChip(
                                    selected = state.selectedPageSize == size,
                                    onClick = { onSelectPageSize(size) },
                                    label = {
                                        Text(
                                            when (size) {
                                                PdfPageSize.AUTO -> stringResource(R.string.export_page_size_auto)
                                                PdfPageSize.A4 -> stringResource(R.string.export_page_size_a4)
                                                PdfPageSize.LETTER -> stringResource(R.string.export_page_size_letter)
                                            },
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                Section(stringResource(R.string.export_image_format_section)) {
                    Text(
                        text = stringResource(
                            if (state.selectedFormat == ExportFormat.PNG) R.string.export_png_description else R.string.export_jpg_description,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun ExportDone(
    files: List<ExportedFile>,
    warning: String?,
    onOpenFile: (ExportedFile) -> Unit,
    onShare: (List<ExportedFile>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        ReplicaScanMascot(ReplicaScanMascotState.Success, size = 140.dp)
        Text(
            text = stringResource(R.string.export_done_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = pluralStringResource(R.plurals.export_done_message, files.size, files.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (warning != null) {
            Text(warning, color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (files.firstOrNull()?.searchableTextIncluded == true) {
            Text(
                text = stringResource(R.string.export_searchable_pdf_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        files.forEach { file ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                Text(file.displayName, modifier = Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.padding(4.dp))
        ReplicaScanPrimaryButton(
            text = stringResource(R.string.export_share_action),
            onClick = { onShare(files) },
            icon = {
                Icon(Icons.Outlined.IosShare, contentDescription = null)
                Spacer(Modifier.width(8.dp))
            },
        )
        if (files.size == 1) {
            ReplicaScanSecondaryButton(
                text = stringResource(R.string.export_open_file_action),
                onClick = { onOpenFile(files.first()) },
                outlined = true,
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(Icons.Outlined.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                },
            )
        }
    }
}

@Composable
private fun ExportProgress(scan: ScanDocument) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("export-progress"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AsyncUriImage(scan.coverPage?.displayUri, fallbackImageUri = scan.coverPage?.sourceUri,
                    modifier = Modifier.size(64.dp, 84.dp), contentScale = ContentScale.Fit, maxDimension = 240)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(scan.title, style = MaterialTheme.typography.titleMedium)
                    Text(pluralStringResource(R.plurals.export_summary, scan.pageCount, scan.pageCount),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        ReplicaScanMascot(ReplicaScanMascotState.Working, size = 208.dp)
        Text(stringResource(R.string.export_progress), style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.export_keep_open), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
