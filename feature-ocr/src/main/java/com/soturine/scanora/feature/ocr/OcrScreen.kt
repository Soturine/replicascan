package com.soturine.scanora.feature.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.soturine.scanora.core.common.model.OcrTextParagraph
import com.soturine.scanora.core.common.model.OcrTextQuality
import com.soturine.scanora.core.ui.component.AsyncUriImage
import com.soturine.scanora.core.ui.component.EmptyStateCard
import com.soturine.scanora.core.ui.component.ScanoraMascot
import com.soturine.scanora.core.ui.component.ScanoraMascotState
import kotlinx.coroutines.launch

private enum class OcrViewMode {
    Paragraphs,
    Continuous,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    state: OcrUiState,
    onRecognizeAgain: () -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copySuccessMessage = stringResource(id = R.string.ocr_copy_success)
    val readableText = remember(state.text) { state.text.trim() }
    val hasReadableText = readableText.isNotBlank()
    var selectedView by rememberSaveable { mutableStateOf(OcrViewMode.Continuous) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.ocr_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (state.page != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                clipboardManager.setText(AnnotatedString(readableText))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(copySuccessMessage)
                                }
                            },
                            enabled = hasReadableText && !state.isLoading,
                        ) {
                            Text(text = stringResource(id = R.string.ocr_copy_all))
                        }
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRecognizeAgain,
                            enabled = !state.isLoading,
                        ) {
                            Text(text = stringResource(id = R.string.ocr_retry))
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        if (state.page == null) {
            EmptyStateCard(
                title = stringResource(id = R.string.ocr_missing_title),
                message = stringResource(id = R.string.ocr_missing_message),
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
                item { OcrHeaderCard(state = state) }
                if (state.quality == OcrTextQuality.WEAK && hasReadableText && !state.isLoading) {
                    item {
                        WeakOcrNotice()
                    }
                }
                if (hasReadableText) {
                    item {
                        OcrViewModeSelector(
                            selectedView = selectedView,
                            onSelectedViewChange = { selectedView = it },
                        )
                    }
                    when (selectedView) {
                        OcrViewMode.Paragraphs -> {
                            itemsIndexed(state.paragraphs) { index, paragraph ->
                                OcrParagraphCard(
                                    index = index,
                                    paragraph = paragraph,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(paragraph.text))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(copySuccessMessage)
                                        }
                                    },
                                )
                            }
                        }
                        OcrViewMode.Continuous -> {
                            item {
                                ContinuousTextCard(text = readableText)
                            }
                        }
                    }
                } else if (!state.isLoading) {
                    item {
                        EmptyStateCard(
                            title = stringResource(id = R.string.ocr_empty_title),
                            message = stringResource(id = R.string.ocr_empty_text),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrHeaderCard(
    state: OcrUiState,
    modifier: Modifier = Modifier,
) {
    val title = when {
        state.isLoading -> stringResource(id = R.string.ocr_processing)
        state.quality == OcrTextQuality.EMPTY -> stringResource(id = R.string.ocr_empty_title)
        state.quality == OcrTextQuality.WEAK -> stringResource(id = R.string.ocr_weak_title)
        else -> stringResource(id = R.string.ocr_result_title)
    }
    val supportingText = when {
        state.isLoading -> stringResource(id = R.string.ocr_processing_detail)
        state.quality == OcrTextQuality.EMPTY -> stringResource(id = R.string.ocr_empty_text)
        state.quality == OcrTextQuality.WEAK -> stringResource(id = R.string.ocr_weak_text)
        else -> pluralStringResource(R.plurals.ocr_supporting, state.paragraphs.size, state.paragraphs.size)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
            if (state.isLoading) {
                ScanoraMascot(
                    state = ScanoraMascotState.Processing,
                    modifier = Modifier.fillMaxWidth(),
                    size = 132.dp,
                )
            }
            AsyncUriImage(
                imageUri = state.previewImageUri ?: state.page?.displayUri.orEmpty(),
                fallbackImageUri = state.page?.sourceUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                contentScale = ContentScale.Fit,
                maxDimension = 1700,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.ocr_eyebrow),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeakOcrNotice(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(id = R.string.ocr_weak_hint),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OcrViewModeSelector(
    selectedView: OcrViewMode,
    onSelectedViewChange: (OcrViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            selected = selectedView == OcrViewMode.Paragraphs,
            onClick = { onSelectedViewChange(OcrViewMode.Paragraphs) },
            label = { Text(text = stringResource(id = R.string.ocr_view_paragraphs)) },
        )
        FilterChip(
            selected = selectedView == OcrViewMode.Continuous,
            onClick = { onSelectedViewChange(OcrViewMode.Continuous) },
            label = { Text(text = stringResource(id = R.string.ocr_view_continuous)) },
        )
    }
}

@Composable
private fun OcrParagraphCard(
    index: Int,
    paragraph: OcrTextParagraph,
    onCopy: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.ocr_paragraph_label, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = onCopy) {
                    Text(text = stringResource(id = R.string.ocr_copy_excerpt))
                }
            }
            SelectionContainer {
                Text(
                    text = paragraph.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ContinuousTextCard(
    text: String,
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
                text = stringResource(id = R.string.ocr_continuous_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
