package com.soturine.scanora.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.usecase.FormatScanDateUseCase
import com.soturine.scanora.core.ui.component.EmptyStateCard
import com.soturine.scanora.core.ui.component.ScanoraMascotState
import com.soturine.scanora.core.ui.component.PageThumbnailCard
import com.soturine.scanora.core.ui.component.SectionHeader
import com.soturine.scanora.core.ui.localizedTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { FormatScanDateUseCase() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.history_title)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionHeader(
                    eyebrow = stringResource(id = R.string.history_eyebrow),
                    title = stringResource(id = R.string.history_heading),
                    supportingText = if (state.scans.isEmpty()) {
                        stringResource(id = R.string.history_supporting_empty)
                    } else {
                        stringResource(id = R.string.history_supporting, state.scans.size)
                    },
                )
            }
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text(text = stringResource(id = R.string.history_search_label)) },
                    placeholder = { Text(text = stringResource(id = R.string.history_search_placeholder)) },
                    singleLine = true,
                )
            }

            if (state.scans.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(id = R.string.history_empty_title),
                        message = stringResource(id = R.string.history_empty_message),
                        mascotState = ScanoraMascotState.Empty,
                    )
                }
            } else {
                items(state.scans, key = { it.id }) { scan ->
                    PageThumbnailCard(
                        title = scan.title,
                        subtitle = stringResource(
                            id = R.string.history_subtitle,
                            scan.pageCount,
                            dateFormatter(scan.updatedAt),
                        ),
                        imageUri = scan.coverPage?.displayUri,
                        fallbackImageUri = scan.coverPage?.sourceUri,
                        overline = scan.mode.localizedTitle(),
                        badge = when {
                            scan.isFavorite -> stringResource(id = R.string.history_badge_favorite)
                            scan.isDraft -> stringResource(id = R.string.history_badge_draft)
                            else -> null
                        },
                        onClick = { onOpenScan(scan.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    scan: ScanDocument?,
    onToggleFavorite: () -> Unit,
    onDeleteScan: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenOcr: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (scan == null) {
        EmptyStateCard(
            title = stringResource(id = R.string.history_detail_missing_title),
            message = stringResource(id = R.string.history_detail_missing_message),
            mascotState = ScanoraMascotState.Attention,
            modifier = modifier.padding(24.dp),
        )
        return
    }

    val dateFormatter = remember { FormatScanDateUseCase() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = scan.title) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionHeader(
                            eyebrow = stringResource(id = R.string.history_detail_eyebrow),
                            title = scan.title,
                            supportingText = stringResource(
                                id = R.string.history_detail_meta,
                                scan.pageCount,
                                dateFormatter(scan.updatedAt),
                            ),
                        )
                        Text(
                            text = if (scan.isDraft) {
                                stringResource(id = R.string.history_detail_draft)
                            } else {
                                stringResource(id = R.string.history_detail_saved)
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onToggleFavorite,
                    ) {
                        Text(
                            text = if (scan.isFavorite) {
                                stringResource(id = R.string.history_remove_favorite)
                            } else {
                                stringResource(id = R.string.history_add_favorite)
                            },
                        )
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenExport,
                    ) {
                        Text(text = stringResource(id = R.string.history_open_export))
                    }
                }
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenReview,
                ) {
                    Text(text = stringResource(id = R.string.history_open_review))
                }
            }
            item {
                SectionHeader(
                    eyebrow = stringResource(id = R.string.history_pages_eyebrow),
                    title = stringResource(id = R.string.history_pages_section),
                    supportingText = stringResource(id = R.string.history_pages_supporting),
                )
            }
            items(scan.pages.sortedBy { it.index }, key = { it.id }) { page ->
                PageThumbnailCard(
                    title = stringResource(id = R.string.history_page_title, page.index + 1),
                    subtitle = page.filterType.localizedTitle(),
                    imageUri = page.displayUri,
                    fallbackImageUri = page.sourceUri,
                    overline = stringResource(id = R.string.history_page_overline),
                    badge = if (page.ocrText.isNullOrBlank()) null else stringResource(id = R.string.history_page_badge_ocr),
                    onClick = { onOpenOcr(page.id) },
                )
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeleteScan,
                ) {
                    Text(text = stringResource(id = R.string.history_delete_scan))
                }
            }
        }
    }
}
