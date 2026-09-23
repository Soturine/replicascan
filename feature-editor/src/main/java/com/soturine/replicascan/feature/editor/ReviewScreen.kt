package com.soturine.replicascan.feature.editor

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.ui.component.AsyncUriImage
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import com.soturine.replicascan.core.ui.component.ReplicaScanToolButton
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: EditorUiState,
    onPreparePreview: (Int) -> Unit,
    onSelectPage: (String) -> Unit,
    onMovePage: (pageId: String, direction: Int) -> Unit,
    onDeleteCurrentPage: () -> Unit,
    onRename: (String) -> Boolean,
    onUpdateTags: (String) -> Unit,
    onRotate: () -> Unit,
    onOpenCrop: (String) -> Unit,
    onOpenFilters: (String) -> Unit,
    onOpenOcr: (String) -> Unit,
    onOpenExport: () -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scan = state.scan
    val page = state.currentPage
    var hadDocument by remember { mutableStateOf(false) }
    if (scan == null || page == null) {
        // Deleting the only page removes the document; leave instead of showing “not found”.
        LaunchedEffect(hadDocument) { if (hadDocument) onBack() }
        if (state.isLoaded && !hadDocument) MissingDocument(onBack, modifier)
        return
    }
    SideEffect { hadDocument = true }
    val pages = remember(scan.pages) { scan.pages.sortedBy { it.index } }
    val position = pages.indexOfFirst { it.id == page.id }
    val snackbarHostState = remember { SnackbarHostState() }
    var previewLongSide by remember(page.id) { mutableIntStateOf(1_600) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    EditorMessageEffect(state.message, snackbarHostState, onClearMessage)
    LaunchedEffect(page.id, page.quad, page.filterType, page.rotationDegrees, previewLongSide) {
        onPreparePreview(previewLongSide)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scan.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(R.string.editor_page_counter, position + 1, pages.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { EditorBackButton(onBack) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(R.string.editor_more_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_details)) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                onClick = {
                                    menuOpen = false
                                    showDetails = true
                                },
                            )
                            if (pages.size > 1) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.editor_move_earlier)) },
                                    enabled = position > 0,
                                    onClick = {
                                        menuOpen = false
                                        onMovePage(page.id, -1)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.editor_move_later)) },
                                    enabled = position < pages.lastIndex,
                                    onClick = {
                                        menuOpen = false
                                        onMovePage(page.id, 1)
                                    },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editor_delete_page)) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val wide = maxWidth >= 600.dp && maxWidth > maxHeight
            val document: @Composable (Modifier) -> Unit = { documentModifier ->
                PagePreview(
                    imageUri = state.previewImageUri ?: page.displayUri,
                    fallbackUri = page.sourceUri,
                    loading = state.isPreviewLoading || state.isProcessing,
                    maxDimension = previewLongSide,
                    modifier = documentModifier.onSizeChanged { previewLongSide = max(it.width, it.height).coerceIn(1_200, 1_800) },
                )
            }
            val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
                ReviewControls(
                    scan = scan,
                    pages = pages,
                    page = page,
                    busy = state.isProcessing,
                    onSelectPage = onSelectPage,
                    onRotate = onRotate,
                    onOpenCrop = { onOpenCrop(page.id) },
                    onOpenFilters = { onOpenFilters(page.id) },
                    onOpenOcr = { onOpenOcr(page.id) },
                    onOpenExport = onOpenExport,
                    modifier = controlsModifier,
                )
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    document(Modifier.weight(1f).fillMaxHeight())
                    controls(Modifier.width(360.dp).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    document(Modifier.weight(1f).fillMaxWidth())
                    controls(Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showDetails) {
        DocumentDetailsDialog(
            scan = scan,
            onSave = { title, tags ->
                if (onRename(title)) {
                    onUpdateTags(tags)
                    showDetails = false
                }
            },
            onDismiss = { showDetails = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, null) },
            title = { Text(stringResource(R.string.editor_delete_page_title)) },
            text = {
                Text(
                    stringResource(
                        if (pages.size == 1) R.string.editor_delete_last_page_message else R.string.editor_delete_page_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteCurrentPage()
                }) { Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PagePreview(
    imageUri: String,
    fallbackUri: String,
    loading: Boolean,
    maxDimension: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = imageUri, label = "pagePreview") { uri ->
            AsyncUriImage(
                imageUri = uri,
                fallbackImageUri = fallbackUri,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                maxDimension = maxDimension,
            )
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun ReviewControls(
    scan: ScanDocument,
    pages: List<ScanPage>,
    page: ScanPage,
    busy: Boolean,
    onSelectPage: (String) -> Unit,
    onRotate: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenOcr: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (pages.size > 1) PageStrip(pages, page.id, onSelectPage)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ReplicaScanToolButton(Icons.Outlined.Crop, stringResource(R.string.editor_tool_crop), onOpenCrop, enabled = !busy)
                ReplicaScanToolButton(Icons.Outlined.Rotate90DegreesCw, stringResource(R.string.editor_tool_rotate), onRotate, enabled = !busy)
                ReplicaScanToolButton(Icons.Outlined.Tune, stringResource(R.string.editor_tool_adjust), onOpenFilters, enabled = !busy)
                ReplicaScanToolButton(Icons.AutoMirrored.Outlined.TextSnippet, stringResource(R.string.editor_tool_text), onOpenOcr, enabled = !busy)
            }
            ReplicaScanPrimaryButton(
                text = stringResource(R.string.editor_export),
                onClick = onOpenExport,
                enabled = scan.pages.isNotEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = {
                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                },
            )
        }
    }
}

@Composable
private fun PageStrip(pages: List<ScanPage>, selectedId: String, onSelectPage: (String) -> Unit) {
    val listState = rememberLazyListState()
    val selectedIndex = pages.indexOfFirst { it.id == selectedId }
    LaunchedEffect(selectedIndex) { if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex) }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(pages, key = { _, item -> item.id }) { index, item ->
            val selected = item.id == selectedId
            val label = stringResource(R.string.editor_page_label, index + 1)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Tab) { onSelectPage(item.id) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        this.selected = selected
                    }
                    .padding(4.dp),
            ) {
                AsyncUriImage(
                    imageUri = item.displayUri,
                    fallbackImageUri = item.sourceUri,
                    modifier = Modifier
                        .size(width = 52.dp, height = 68.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.extraSmall,
                        ),
                    contentScale = ContentScale.Crop,
                    maxDimension = 320,
                )
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DocumentDetailsDialog(
    scan: ScanDocument,
    onSave: (title: String, tags: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable(scan.id) { mutableStateOf(scan.title) }
    var tags by rememberSaveable(scan.id) { mutableStateOf(scan.tags.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.editor_document_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.editor_tags)) },
                    supportingText = { Text(stringResource(R.string.editor_tags_helper)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, tags) }) { Text(stringResource(R.string.editor_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
