package com.soturine.replicascan.feature.export

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
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
                            modifier = Modifier.testTag("export-action"),
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
                ExportDone(scan, state.exportedFiles, warning = messageText, onOpenFile = onOpenFile, onShare = onShare)
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
            state.scan?.let { scan ->
                ExportDocumentStage(
                    scan = scan,
                    mascotState = if (errorText == null) ReplicaScanMascotState.Ocr else ReplicaScanMascotState.Attention,
                )
                Text(
                    pluralStringResource(R.plurals.export_summary, pageCount, pageCount),
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (errorText != null) {
                Column(
                    Modifier.fillMaxWidth().background(
                        MaterialTheme.colorScheme.error.copy(alpha = .08f),
                        MaterialTheme.shapes.medium,
                    ).padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.export_recovery), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error)
                    Text(errorText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            ExportFilenameField(fileName, onNameChange, nameValid)
            Section(stringResource(R.string.export_format_section)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ExportFormat.entries.forEach { format ->
                        ExportFormatChoice(
                            format = format,
                            selected = state.selectedFormat == format,
                            onClick = { onSelectFormat(format) },
                            modifier = Modifier.weight(1f).testTag("format-${format.name}"),
                        )
                    }
                }
            }
            if (isPdf) {
                Surface(
                    onClick = { moreOptions = !moreOptions },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("export-quality-disclosure"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.export_quality_section), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.selectedQuality.localizedTitle(), style = MaterialTheme.typography.titleMedium)
                            Text(state.selectedQuality.localizedDescription(), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (moreOptions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null)
                    }
                }
                AnimatedVisibility(visible = moreOptions) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
            } else {
                Text(
                    text = stringResource(
                        if (state.selectedFormat == ExportFormat.PNG) R.string.export_png_description else R.string.export_jpg_description,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExportFilenameField(value: String, onValueChange: (String) -> Unit, isValid: Boolean) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.export_filename)
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().clip(shape)
                .background(colors.surfaceContainerLow)
                .border(1.dp, if (isValid) colors.outlineVariant else colors.error, shape)
                .testTag("export-filename")
                .semantics { contentDescription = label },
            textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            decorationBox = { innerTextField ->
                Column(Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) Text(label, style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant)
                        innerTextField()
                    }
                }
            },
        )
        if (!isValid) Text(stringResource(R.string.export_filename_invalid),
            style = MaterialTheme.typography.bodySmall, color = colors.error)
    }
}

@Composable
private fun ExportFormatChoice(format: ExportFormat, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        border = BorderStroke(1.dp, if (selected) colors.primary.copy(alpha = .55f) else colors.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        ) {
            Text(format.title, style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
            Box(Modifier.width(24.dp).height(2.dp).background(
                if (selected) colors.primary else colors.outlineVariant,
                RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun ExportDocumentStage(scan: ScanDocument, mascotState: ReplicaScanMascotState) {
    val colors = MaterialTheme.colorScheme
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val isDark = colors.background.luminance() < .5f
    val paperColor = if (isDark) colors.inverseSurface else colors.surface
    val paperInk = if (isDark) colors.inverseOnSurface else colors.onSurface
    val stageShape = RoundedCornerShape(26.dp)
    val pageShape = RoundedCornerShape(10.dp)
    val pageAlignment = if (largeText) Alignment.Center else Alignment.CenterStart
    val pageInset = if (largeText) Modifier else Modifier.padding(start = 32.dp)
    val pageWidth = if (largeText) 130.dp else 148.dp
    val pageHeight = if (largeText) 164.dp else 190.dp
    Box(
        Modifier.fillMaxWidth().height(if (largeText) 188.dp else 228.dp)
            .clip(stageShape).background(colors.surfaceContainerHigh),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                colors.primaryContainer.copy(alpha = .6f),
                radius = size.width * .45f,
                center = Offset(size.width * .77f, size.height * .23f),
            )
            drawLine(colors.outlineVariant.copy(alpha = .55f),
                Offset(0f, size.height * .9f), Offset(size.width, size.height * .9f),
                strokeWidth = 2.dp.toPx())
        }
        if (scan.pageCount > 1) {
            Box(
                Modifier.align(pageAlignment).then(pageInset)
                    .offset(x = 12.dp, y = (-7).dp)
                    .size(width = pageWidth, height = pageHeight)
                    .graphicsLayer { rotationZ = if (largeText) 0f else -2f }
                    .shadow(8.dp, pageShape)
                    .background(paperColor, pageShape)
                    .border(1.dp, paperInk.copy(alpha = .13f), pageShape),
            )
        }
        Box(
            modifier = Modifier.align(pageAlignment).then(pageInset)
                .size(width = pageWidth, height = pageHeight)
                .graphicsLayer { rotationZ = if (largeText) 0f else -6f }
                .shadow(12.dp, pageShape)
                .background(paperColor, pageShape)
                .border(1.dp, paperInk.copy(alpha = .16f), pageShape)
                .padding(8.dp),
        ) {
            AsyncUriImage(
                scan.coverPage?.displayUri,
                fallbackImageUri = scan.coverPage?.sourceUri,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Fit,
                maxDimension = 400,
            )
        }
        if (!largeText) {
            ReplicaScanMascot(
                mascotState,
                size = 174.dp,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-8).dp),
            )
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
    scan: ScanDocument,
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
        ExportDocumentStage(scan, ReplicaScanMascotState.Success)
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
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        ExportDocumentStage(scan, ReplicaScanMascotState.Working)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(scan.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(pluralStringResource(R.plurals.export_summary, scan.pageCount, scan.pageCount),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.export_progress), style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.export_keep_open), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
