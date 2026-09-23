package com.soturine.replicascan.feature.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.util.DateFormatter
import com.soturine.replicascan.core.ui.component.AsyncUriImage
import com.soturine.replicascan.core.ui.component.DocumentListItem
import com.soturine.replicascan.core.ui.component.EmptyStateCard
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import com.soturine.replicascan.core.ui.component.ReplicaScanSecondaryButton
import java.util.Locale
import java.util.TimeZone

@Composable
private fun rememberDateFormatter(): DateFormatter {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(locale) { DateFormatter(locale, TimeZone.getDefault()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onOpenScan: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberDateFormatter()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.history_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "search") {
                ReplicaScanContent {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.query,
                        onValueChange = onQueryChange,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                }
            }
            if (state.isLoaded && state.scans.isEmpty()) {
                item(key = "empty") {
                    ReplicaScanContent(modifier = Modifier.padding(top = 16.dp)) {
                        if (state.query.isBlank()) {
                            EmptyStateCard(
                                title = stringResource(R.string.history_empty_title),
                                message = stringResource(R.string.history_empty_message),
                                mascotState = ReplicaScanMascotState.Empty,
                            )
                        } else {
                            EmptyStateCard(
                                title = stringResource(R.string.history_no_results_title),
                                message = stringResource(R.string.history_no_results_message),
                            )
                        }
                    }
                }
            }
            items(state.scans, key = { it.id }) { scan ->
                ReplicaScanContent(modifier = Modifier.animateItem()) {
                    DocumentListItem(
                        title = scan.title,
                        supportingText = scan.summary(formatter),
                        imageUri = scan.coverPage?.displayUri,
                        fallbackImageUri = scan.coverPage?.sourceUri,
                        onClick = { onOpenScan(scan.id) },
                        trailing = if (scan.isFavorite) {
                            {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.history_favorite),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanDocument.summary(formatter: DateFormatter): String =
    pluralStringResource(R.plurals.history_pages, pageCount, pageCount) + " · " + formatter.format(updatedAt)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scan: ScanDocument?,
    isLoaded: Boolean,
    onToggleFavorite: () -> Unit,
    onDeleteScan: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenOcr: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val formatter = rememberDateFormatter()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(scan?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.history_back))
                    }
                },
                actions = {
                    if (scan != null) {
                        IconToggleButton(checked = scan.isFavorite, onCheckedChange = { onToggleFavorite() }) {
                            Icon(
                                if (scan.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(R.string.history_favorite),
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.history_delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (scan != null) {
                Surface {
                    Column(
                        modifier = Modifier.navigationBarsPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReplicaScanPrimaryButton(
                            text = stringResource(R.string.history_export),
                            onClick = onOpenExport,
                            icon = {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            },
                        )
                        ReplicaScanSecondaryButton(
                            text = stringResource(R.string.history_edit),
                            onClick = onOpenReview,
                            outlined = true,
                            modifier = Modifier.fillMaxWidth(),
                            icon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (scan == null) {
            if (isLoaded) {
                EmptyStateCard(
                    title = stringResource(R.string.history_missing_title),
                    message = stringResource(R.string.history_missing_message),
                    mascotState = ReplicaScanMascotState.Attention,
                    modifier = Modifier.padding(innerPadding).padding(24.dp),
                )
            }
            return@Scaffold
        }
        val pages = remember(scan.pages) { scan.pages.sortedBy { it.index } }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = scan.summary(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(pages, key = { it.id }) { page ->
                val label = stringResource(R.string.history_page_label, page.index + 1)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onOpenOcr(page.id) }
                        .semantics(mergeDescendants = true) { contentDescription = label }
                        .padding(4.dp),
                ) {
                    AsyncUriImage(
                        imageUri = page.displayUri,
                        fallbackImageUri = page.sourceUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(MaterialTheme.shapes.small)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                        maxDimension = 480,
                    )
                    Text("${page.index + 1}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (confirmDelete && scan != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteScan()
                }) { Text(stringResource(R.string.history_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}
