package com.soturine.scanora.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PointValue
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.ui.component.AsyncUriImage
import com.soturine.scanora.core.ui.component.EmptyStateCard
import com.soturine.scanora.core.ui.component.SectionHeader
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    state: EditorUiState,
    onSaveQuadAndContinue: (DocumentQuad) -> Unit,
    onEnsureQuad: () -> Unit,
    onReestimate: () -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = state.currentPage
    val snackbarHostState = remember { SnackbarHostState() }
    var localQuad by remember(page?.id, page?.quad) {
        mutableStateOf(page?.quad ?: defaultQuad())
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }

    LaunchedEffect(page?.id, page?.quad) {
        if (page != null && page.quad == null) {
            onEnsureQuad()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.editor_crop_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.editor_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (page != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isProcessing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = onReestimate,
                                enabled = !state.isProcessing,
                            ) {
                                Text(text = stringResource(id = R.string.editor_reestimate_crop))
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onSaveQuadAndContinue(localQuad) },
                                enabled = !state.isProcessing,
                            ) {
                                Text(text = stringResource(id = R.string.editor_save_crop_continue))
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (page == null) {
            EmptyStateCard(
                title = stringResource(id = R.string.editor_missing_page_title),
                message = stringResource(id = R.string.editor_missing_page_message),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader(
                    eyebrow = stringResource(id = R.string.editor_crop_eyebrow),
                    title = stringResource(id = R.string.editor_crop_heading),
                    supportingText = stringResource(id = R.string.editor_crop_helper),
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DocumentCropPreview(
                            imageUri = page.sourceUri,
                            quad = localQuad,
                            onQuadChange = { localQuad = it },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (state.isProcessing) {
                            PreviewProgressOverlay(
                                text = stringResource(id = R.string.editor_crop_detecting),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(id = R.string.editor_crop_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    state: EditorUiState,
    onApplyFilter: (DocumentFilterType) -> Unit,
    onRequestPreview: (DocumentFilterType, Int) -> Unit,
    onRotate: () -> Unit,
    onOpenReview: () -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val page = state.currentPage
    var selectedFilter by remember(page?.id, page?.filterType) {
        mutableStateOf(page?.filterType ?: DocumentFilterType.ORIGINAL_CORRECTED)
    }
    var previewLongSide by remember(page?.id) { mutableIntStateOf(1500) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }

    LaunchedEffect(page?.id, selectedFilter, previewLongSide) {
        if (page != null) {
            onRequestPreview(selectedFilter, previewLongSide)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.editor_filter_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.editor_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (page != null) {
                val canContinue = selectedFilter == page.filterType && page.processedUri != null
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isProcessing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f),
                                onClick = onRotate,
                                enabled = !state.isProcessing,
                            ) {
                                Text(text = stringResource(id = R.string.editor_rotate))
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (canContinue) {
                                        onOpenReview()
                                    } else {
                                        onApplyFilter(selectedFilter)
                                    }
                                },
                                enabled = !state.isProcessing,
                            ) {
                                Text(
                                    text = if (canContinue) {
                                        stringResource(id = R.string.editor_save_continue)
                                    } else {
                                        stringResource(id = R.string.editor_apply_visual)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (page == null) {
            EmptyStateCard(
                title = stringResource(id = R.string.editor_missing_page_title),
                message = stringResource(id = R.string.editor_missing_page_message),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SectionHeader(
                        eyebrow = stringResource(id = R.string.editor_filter_eyebrow),
                        title = stringResource(id = R.string.editor_filter_heading),
                        supportingText = stringResource(id = R.string.editor_filter_helper),
                    )
                }
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
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(396.dp)
                                    .onSizeChanged { size ->
                                        previewLongSide = max(size.width, size.height).coerceIn(1400, 1800)
                                    },
                            ) {
                                AsyncUriImage(
                                    imageUri = state.previewImageUri ?: page.displayUri,
                                    fallbackImageUri = page.sourceUri,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    maxDimension = previewLongSide,
                                )
                                if (state.isPreviewLoading || state.isProcessing) {
                                    PreviewProgressOverlay(
                                        text = if (state.isProcessing) {
                                            stringResource(id = R.string.editor_filter_applying)
                                        } else {
                                            stringResource(id = R.string.editor_filter_preview_loading)
                                        },
                                    )
                                }
                            }
                            Text(
                                text = if (state.isPreviewRefining && !state.isProcessing) {
                                    stringResource(id = R.string.editor_filter_preview_refining)
                                } else {
                                    selectedFilter.description()
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(DocumentFilterType.entries, key = { _, filter -> filter.storageKey }) { _, filter ->
                            FilterPresetCard(
                                title = filter.title,
                                selected = selectedFilter == filter,
                                current = page.filterType == filter,
                                enabled = !state.isProcessing,
                                onClick = { selectedFilter = filter },
                            )
                        }
                    }
                }
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
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = selectedFilter.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = selectedFilter.description(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: EditorUiState,
    onRename: (String) -> Unit,
    onUpdateTags: (String) -> Unit,
    onPreparePreview: (Int) -> Unit,
    onClearMessage: () -> Unit,
    onSelectPage: (String) -> Unit,
    onMovePageUp: (String) -> Unit,
    onMovePageDown: (String) -> Unit,
    onDeleteCurrentPage: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenOcr: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scan = state.scan
    if (scan == null) {
        EmptyStateCard(
            title = stringResource(id = R.string.editor_missing_page_title),
            message = stringResource(id = R.string.editor_missing_page_message),
            modifier = modifier.padding(24.dp),
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val orderedPages = remember(scan.pages) { scan.pages.sortedBy { it.index } }
    val selectedPage = state.currentPage ?: orderedPages.firstOrNull()
    val persistedTags = remember(scan.tags) { scan.tags.joinToString(", ") }
    var title by rememberSaveable(scan.id, scan.title) { mutableStateOf(scan.title) }
    var tags by rememberSaveable(scan.id, persistedTags) { mutableStateOf(persistedTags) }
    var metadataExpanded by rememberSaveable(scan.id) { mutableStateOf(false) }
    var previewLongSide by remember(selectedPage?.id) { mutableIntStateOf(1600) }
    val normalizedTagDraft = remember(tags) {
        tags
            .split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }
    val hasMetadataChanges = title.trim() != scan.title || normalizedTagDraft != scan.tags

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }

    LaunchedEffect(
        selectedPage?.id,
        selectedPage?.quad,
        selectedPage?.filterType,
        selectedPage?.rotationDegrees,
        previewLongSide,
    ) {
        if (selectedPage != null) {
            onPreparePreview(previewLongSide)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.editor_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.editor_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenExport,
                    ) {
                        Text(text = stringResource(id = R.string.editor_open_export))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onOpenCrop,
                            enabled = selectedPage != null,
                        ) {
                            Text(text = stringResource(id = R.string.editor_open_crop))
                        }
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onOpenFilters,
                            enabled = selectedPage != null,
                        ) {
                            Text(text = stringResource(id = R.string.editor_open_filters))
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.editor_review_summary, orderedPages.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (scan.tags.isNotEmpty()) {
                            Text(
                                text = scan.tags.joinToString(separator = " / "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        OutlinedButton(onClick = { metadataExpanded = !metadataExpanded }) {
                            Text(
                                text = if (metadataExpanded) {
                                    stringResource(id = R.string.editor_hide_metadata)
                                } else {
                                    stringResource(id = R.string.editor_edit_metadata)
                                },
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(visible = metadataExpanded || hasMetadataChanges) {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(text = stringResource(id = R.string.editor_document_name)) },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = tags,
                                onValueChange = { tags = it },
                                label = { Text(text = stringResource(id = R.string.editor_tags)) },
                                supportingText = {
                                    Text(text = stringResource(id = R.string.editor_tags_helper))
                                },
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onRename(title)
                                    onUpdateTags(tags)
                                    metadataExpanded = false
                                },
                                enabled = hasMetadataChanges,
                            ) {
                                Text(text = stringResource(id = R.string.editor_save_metadata))
                            }
                        }
                    }
                }
            }
            if (selectedPage != null) {
                item {
                    SelectedPageCard(
                        page = selectedPage,
                        imageUri = state.previewImageUri ?: selectedPage.displayUri,
                        pageNumber = selectedPage.index + 1,
                        canMoveUp = orderedPages.firstOrNull()?.id != selectedPage.id,
                        canMoveDown = orderedPages.lastOrNull()?.id != selectedPage.id,
                        onPreviewSize = { size ->
                            previewLongSide = max(size.width, size.height).coerceIn(1400, 1800)
                        },
                        onOpenOcr = { onOpenOcr(selectedPage.id) },
                        onMoveUp = { onMovePageUp(selectedPage.id) },
                        onMoveDown = { onMovePageDown(selectedPage.id) },
                        onDelete = onDeleteCurrentPage,
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.editor_pages_section),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(
                        items = orderedPages,
                        key = { _, page -> page.id },
                    ) { _, page ->
                        ReviewPageStripItem(
                            page = page,
                            selected = selectedPage?.id == page.id,
                            onClick = { onSelectPage(page.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPresetCard(
    title: String,
    selected: Boolean,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(176.dp)
            .alpha(if (enabled) 1f else 0.58f)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                },
                shape = MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        onClick = { if (enabled) onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when {
                    current && selected -> stringResource(id = R.string.editor_filter_selected_current)
                    current -> stringResource(id = R.string.editor_filter_current)
                    selected -> stringResource(id = R.string.editor_filter_selected)
                    else -> stringResource(id = R.string.editor_filter_preview_label)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SelectedPageCard(
    page: ScanPage,
    imageUri: String,
    pageNumber: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPreviewSize: (IntSize) -> Unit,
    onOpenOcr: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(id = R.string.editor_page_title, pageNumber),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = page.filterType.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!page.ocrText.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.editor_ocr_ready),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                FilledTonalButton(onClick = onOpenOcr) {
                    Text(text = stringResource(id = R.string.editor_open_ocr))
                }
            }
            AsyncUriImage(
                imageUri = imageUri,
                fallbackImageUri = page.sourceUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(288.dp)
                    .onSizeChanged(onPreviewSize),
                contentScale = ContentScale.Fit,
                maxDimension = 1800,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDelete,
                ) {
                    Text(text = stringResource(id = R.string.editor_delete_page))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                ) {
                    Text(text = stringResource(id = R.string.editor_move_up))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                ) {
                    Text(text = stringResource(id = R.string.editor_move_down))
                }
            }
        }
    }
}

@Composable
private fun ReviewPageStripItem(
    page: ScanPage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(148.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                },
                shape = MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncUriImage(
                imageUri = page.displayUri,
                fallbackImageUri = page.sourceUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                contentScale = ContentScale.Fit,
                maxDimension = 1200,
            )
            Text(
                text = stringResource(id = R.string.editor_page_title, page.index + 1),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = page.filterType.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DocumentCropPreview(
    imageUri: String,
    quad: DocumentQuad,
    onQuadChange: (DocumentQuad) -> Unit,
    modifier: Modifier = Modifier,
) {
    var imageSize by remember(imageUri) { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = modifier) {
        val imageBounds = remember(
            constraints.maxWidth,
            constraints.maxHeight,
            imageSize,
        ) {
            computeFittedBounds(
                containerWidth = constraints.maxWidth.toFloat(),
                containerHeight = constraints.maxHeight.toFloat(),
                imageSize = imageSize,
            )
        }

        AsyncUriImage(
            imageUri = imageUri,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            maxDimension = 1600,
            onBitmapLoaded = { imageSize = it },
        )

        imageBounds?.let { bounds ->
            QuadEditorOverlay(
                quad = quad,
                imageBounds = bounds,
                onQuadChange = onQuadChange,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PreviewProgressOverlay(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun computeFittedBounds(
    containerWidth: Float,
    containerHeight: Float,
    imageSize: IntSize,
): Rect? {
    if (containerWidth <= 0f || containerHeight <= 0f) return null
    if (imageSize == IntSize.Zero || imageSize.width == 0 || imageSize.height == 0) return null

    val imageAspect = imageSize.width.toFloat() / imageSize.height.toFloat()
    val containerAspect = containerWidth / containerHeight

    return if (imageAspect > containerAspect) {
        val fittedHeight = containerWidth / imageAspect
        val top = (containerHeight - fittedHeight) / 2f
        Rect(
            left = 0f,
            top = top,
            right = containerWidth,
            bottom = top + fittedHeight,
        )
    } else {
        val fittedWidth = containerHeight * imageAspect
        val left = (containerWidth - fittedWidth) / 2f
        Rect(
            left = left,
            top = 0f,
            right = left + fittedWidth,
            bottom = containerHeight,
        )
    }
}

private fun defaultQuad(): DocumentQuad =
    DocumentQuad(
        topLeft = PointValue(0.08f, 0.08f),
        topRight = PointValue(0.92f, 0.08f),
        bottomRight = PointValue(0.92f, 0.92f),
        bottomLeft = PointValue(0.08f, 0.92f),
    )

private fun DocumentFilterType.description(): String = when (this) {
    DocumentFilterType.ORIGINAL_CORRECTED -> "Mantém o papel mais natural, corrige a perspectiva e limpa sem exagero."
    DocumentFilterType.DOCUMENT_BLACK_WHITE -> "Focado em texto e impressão, sem apagar o miolo da página."
    DocumentFilterType.DOCUMENT_GRAY -> "Deixa a leitura mais confortável sem dar aspecto metálico ao documento."
    DocumentFilterType.COLOR_ENHANCED -> "Preserva cor, marca-texto e caneta com contraste mais limpo."
    DocumentFilterType.RECEIPT_HIGH_CONTRAST -> "Focado em recibos, notas térmicas e papéis mais apagados."
}
