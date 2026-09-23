package com.soturine.replicascan.feature.export

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onExport: () -> Unit,
    onShare: (List<ExportedFile>) -> Unit,
    onOpenFile: (ExportedFile) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { message ->
        when (message) {
            ExportMessage.EMPTY_DOCUMENT -> stringResource(R.string.export_message_empty)
            ExportMessage.PAGE_UNREADABLE -> state.failedPageNumber
                ?.let { stringResource(R.string.export_message_page_unreadable, it) }
                ?: stringResource(R.string.export_message_failed)
            ExportMessage.WRITE_FAILED -> stringResource(R.string.export_message_failed)
        }
    }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onClearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.export_back))
                    }
                },
            )
        },
        bottomBar = {
            val scan = state.scan
            if (scan != null && state.exportedFiles.isEmpty()) {
                Surface {
                    Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                        if (state.isExporting) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp))
                        ReplicaScanPrimaryButton(
                            text = stringResource(R.string.export_action),
                            onClick = onExport,
                            enabled = !state.isExporting && scan.pages.isNotEmpty(),
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
            targetState = state.exportedFiles.isNotEmpty(),
            label = "exportState",
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) { done ->
            if (done) {
                ExportDone(state.exportedFiles, onOpenFile = onOpenFile, onShare = onShare)
            } else {
                ExportOptions(
                    state = state,
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
            Text(
                text = pluralStringResource(R.plurals.export_summary, pageCount, pageCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Section(stringResource(R.string.export_format_section)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { index, pdf ->
                        SegmentedButton(
                            selected = isPdf == pdf,
                            onClick = { onSelectFormat(if (pdf) ExportFormat.PDF else ExportFormat.JPG) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(if (pdf) R.string.export_kind_pdf else R.string.export_kind_image))
                        }
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ExportFormat.JPG, ExportFormat.PNG).forEach { format ->
                            FilterChip(
                                selected = state.selectedFormat == format,
                                onClick = { onSelectFormat(format) },
                                label = { Text(format.title) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
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
        if (files.firstOrNull()?.searchableTextIncluded == true) {
            Text(
                text = stringResource(R.string.export_searchable_pdf_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
