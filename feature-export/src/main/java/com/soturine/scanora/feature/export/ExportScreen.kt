package com.soturine.scanora.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soturine.scanora.core.common.model.ExportFormat
import com.soturine.scanora.core.common.model.ExportedFile
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.PdfPageSize
import com.soturine.scanora.core.ui.localizedDescription
import com.soturine.scanora.core.ui.localizedTitle
import com.soturine.scanora.core.ui.component.EmptyStateCard
import com.soturine.scanora.core.ui.component.SectionHeader
import com.soturine.scanora.core.ui.component.ScanoraMascot
import com.soturine.scanora.core.ui.component.ScanoraMascotState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    state: ExportUiState,
    onSelectFormat: (ExportFormat) -> Unit,
    onSelectQuality: (PdfQuality) -> Unit,
    onSelectPageSize: (PdfPageSize) -> Unit,
    onExport: () -> Unit,
    onShare: (List<ExportedFile>) -> Unit,
    onOpenFile: (ExportedFile) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val exportSuccessMessage = if (state.exportedFiles.size == 1) {
        stringResource(id = R.string.export_success_snackbar_single)
    } else {
        stringResource(id = R.string.export_success_snackbar_multiple, state.exportedFiles.size)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }
    LaunchedEffect(state.exportedFiles) {
        if (state.exportedFiles.isNotEmpty()) {
            snackbarHostState.showSnackbar(exportSuccessMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.export_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (state.scan != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isExporting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else if (state.exportedFiles.isEmpty()) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onExport,
                                enabled = !state.isExporting && state.scan.pages.isNotEmpty(),
                            ) {
                                Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (state.selectedFormat == ExportFormat.PDF) {
                                        stringResource(id = R.string.export_action_pdf)
                                    } else {
                                        stringResource(id = R.string.export_action_images)
                                    },
                                )
                            }
                        } else {
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onShare(state.exportedFiles) },
                            ) {
                                Icon(Icons.Outlined.IosShare, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(text = stringResource(id = R.string.export_share_action))
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val scan = state.scan
        if (scan == null) {
            EmptyStateCard(
                title = stringResource(id = R.string.export_missing_title),
                message = stringResource(id = R.string.export_missing_message),
                mascotState = ScanoraMascotState.Attention,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp),
            )
        } else {
            val selectedKind = state.selectedFormat.exportKind()
            if (state.isExporting) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ScanoraMascot(ScanoraMascotState.Processing, size = 220.dp, showLabel = true)
                    Text(
                        text = stringResource(R.string.export_processing_message),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.exportedFiles.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ScanoraMascot(ScanoraMascotState.Success, size = 220.dp)
                    SectionHeader(
                        eyebrow = stringResource(R.string.export_ready_eyebrow),
                        title = stringResource(R.string.export_ready_title),
                        supportingText = pluralStringResource(R.plurals.export_success_message, state.exportedFiles.size, state.exportedFiles.size),
                    )
                    if (state.exportedFiles.first().searchableTextIncluded) {
                        Text(
                            text = stringResource(R.string.export_searchable_pdf_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        onClick = { onOpenFile(state.exportedFiles.first()) },
                    ) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.export_open_file_action))
                    }
                }
            } else LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SectionHeader(
                                eyebrow = stringResource(id = R.string.export_eyebrow),
                                title = scan.title,
                                supportingText = pluralStringResource(R.plurals.export_summary, scan.pageCount, scan.pageCount),
                            )
                        }
                    }
                }
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.export_format_section),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ExportKind.entries.forEach { kind ->
                                ExportChoiceButton(
                                    modifier = Modifier.weight(1f),
                                    title = kind.title(),
                                    selected = selectedKind == kind,
                                    onClick = {
                                        onSelectFormat(
                                            when (kind) {
                                                ExportKind.PDF -> ExportFormat.PDF
                                                ExportKind.IMAGE ->
                                                    state.selectedFormat.takeIf { it.exportKind() == ExportKind.IMAGE }
                                                        ?: ExportFormat.JPG
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        Text(
                            text = selectedKind.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (selectedKind == ExportKind.IMAGE) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.export_image_format_section),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    listOf(ExportFormat.JPG, ExportFormat.PNG).forEach { format ->
                                        ExportChoiceButton(
                                            modifier = Modifier.weight(1f),
                                            title = format.title,
                                            selected = state.selectedFormat == format,
                                            onClick = { onSelectFormat(format) },
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(id = R.string.export_image_format_supporting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (state.selectedFormat == ExportFormat.PDF) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.export_quality_section),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(end = 8.dp),
                                ) {
                                    items(PdfQuality.entries, key = { it.name }) { quality ->
                                        ExportChoiceButton(
                                            modifier = Modifier.widthIn(min = 124.dp),
                                            title = quality.localizedTitle(),
                                            selected = state.selectedQuality == quality,
                                            onClick = { onSelectQuality(quality) },
                                        )
                                    }
                                }
                                Text(
                                    text = state.selectedQuality.localizedDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.export_page_size_section),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(end = 8.dp),
                                ) {
                                    items(PdfPageSize.entries, key = { it.name }) { size ->
                                        ExportChoiceButton(
                                            modifier = Modifier.widthIn(min = 112.dp),
                                            title = when (size) {
                                                PdfPageSize.AUTO -> stringResource(R.string.export_page_size_auto)
                                                PdfPageSize.A4 -> "A4"
                                                PdfPageSize.LETTER -> stringResource(R.string.export_page_size_letter)
                                            },
                                            selected = state.selectedPageSize == size,
                                            onClick = { onSelectPageSize(size) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportChoiceButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        modifier = modifier.heightIn(min = 52.dp),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ExportedFileCard(
    file: ExportedFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ExportMetadataLine(
                label = stringResource(id = R.string.export_file_type),
                value = file.mimeType.typeLabel(),
            )
            ExportMetadataLine(
                label = stringResource(id = R.string.export_file_size),
                value = file.sizeLabel(),
            )
            ExportMetadataLine(
                label = stringResource(id = R.string.export_file_location),
                value = file.locationLabel,
            )
            file.pathHint?.takeIf { it.isNotBlank() }?.let { pathHint ->
                ExportMetadataLine(
                    label = stringResource(id = R.string.export_file_path),
                    value = pathHint,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = onOpen,
                ) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.export_open_file_action))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onShare,
                ) {
                    Icon(Icons.Outlined.IosShare, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.export_share_file_action))
                }
            }
        }
    }
}

@Composable
private fun ExportMetadataLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

private enum class ExportKind {
    PDF,
    IMAGE,
}

private fun ExportFormat.exportKind(): ExportKind =
    if (this == ExportFormat.PDF) ExportKind.PDF else ExportKind.IMAGE

@Composable
private fun ExportKind.title(): String = when (this) {
    ExportKind.PDF -> stringResource(id = R.string.export_kind_pdf)
    ExportKind.IMAGE -> stringResource(id = R.string.export_kind_image)
}

@Composable
private fun ExportKind.description(): String = stringResource(when (this) {
    ExportKind.PDF -> R.string.export_kind_pdf_description
    ExportKind.IMAGE -> R.string.export_kind_image_description
})

@Composable
private fun String.typeLabel(): String = stringResource(when (this) {
    ExportFormat.PDF.mimeType -> R.string.export_type_pdf
    ExportFormat.JPG.mimeType -> R.string.export_type_jpg
    ExportFormat.PNG.mimeType -> R.string.export_type_png
    else -> return this
})

private fun ExportedFile.sizeLabel(): String {
    val sizeInKb = sizeBytes / 1024f
    return if (sizeInKb >= 1024f) {
        String.format("%.1f MB", sizeInKb / 1024f)
    } else {
        String.format("%.0f KB", sizeInKb.coerceAtLeast(1f))
    }
}
