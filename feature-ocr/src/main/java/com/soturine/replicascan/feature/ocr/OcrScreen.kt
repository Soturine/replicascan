package com.soturine.replicascan.feature.ocr

import android.content.ClipData
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrTextQuality
import com.soturine.replicascan.core.ui.component.EmptyStateCard
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    state: OcrUiState,
    onRetry: () -> Unit,
    onScriptSelected: (OcrScript) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.ocr_copied)
    val messageText = state.message?.let { message ->
        stringResource(
            when (message) {
                OcrMessage.IMAGE_UNREADABLE -> R.string.ocr_message_image_unreadable
                OcrMessage.MODEL_PREPARING -> R.string.ocr_preparing
                OcrMessage.FAILED -> R.string.ocr_message_failed
            },
        )
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
                title = { Text(stringResource(R.string.ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.ocr_back))
                    }
                },
            )
        },
        bottomBar = {
            if (state.text.isNotBlank()) {
                Surface {
                    ReplicaScanPrimaryButton(
                        text = stringResource(R.string.ocr_copy),
                        onClick = {
                            coroutineScope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, state.text)))
                                // Android 13+ shows its own clipboard confirmation.
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    snackbarHostState.showSnackbar(copiedMessage)
                                }
                            }
                        },
                        enabled = !state.isRecognizing,
                        modifier = Modifier.navigationBarsPadding().padding(16.dp),
                        icon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.isLoaded && state.page == null) {
            EmptyStateCard(
                title = stringResource(R.string.ocr_missing_title),
                message = stringResource(R.string.ocr_missing_message),
                mascotState = ReplicaScanMascotState.Attention,
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                state.isRecognizing -> item(key = "working") {
                    ReplicaScanContent { RecognizingState(state.readiness) }
                }
                state.text.isBlank() -> item(key = "empty") {
                    ReplicaScanContent {
                        EmptyStateCard(
                            title = stringResource(R.string.ocr_empty_title),
                            message = stringResource(R.string.ocr_empty_message),
                            mascotState = ReplicaScanMascotState.Ocr,
                        )
                    }
                }
                else -> {
                    if (state.quality == OcrTextQuality.WEAK || state.quality == OcrTextQuality.PARTIAL) {
                        item(key = "quality") {
                            ReplicaScanContent {
                                Text(
                                    text = stringResource(R.string.ocr_quality_uncertain),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(state.paragraphs.ifEmpty { null }?.map { it.text } ?: listOf(state.text)) { paragraph ->
                        ReplicaScanContent {
                            SelectionContainer {
                                Text(paragraph, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            item(key = "language") {
                ReplicaScanContent {
                    LanguageOverride(
                        selected = state.script,
                        enabled = !state.isRecognizing,
                        onSelected = onScriptSelected,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizingState(readiness: OcrModelReadiness) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReplicaScanMascot(ReplicaScanMascotState.Ocr, size = 120.dp)
        Text(
            text = stringResource(
                if (readiness == OcrModelReadiness.DOWNLOAD_PENDING) R.string.ocr_preparing else R.string.ocr_reading,
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
    }
}

/** Progressive disclosure: the recognizer is picked automatically; changing it is an advanced action. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LanguageOverride(
    selected: OcrScript,
    enabled: Boolean,
    onSelected: (OcrScript) -> Unit,
    onRetry: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(R.string.ocr_try_other_language))
            Spacer(Modifier.width(4.dp))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.ocr_language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrScript.entries.forEach { script ->
                        FilterChip(
                            selected = selected == script,
                            onClick = { onSelected(script) },
                            enabled = enabled,
                            label = { Text(script.label()) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                TextButton(onClick = onRetry, enabled = enabled) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ocr_retry))
                }
            }
        }
    }
}

@Composable
private fun OcrScript.label(): String = stringResource(
    when (this) {
        OcrScript.LATIN -> R.string.ocr_script_latin
        OcrScript.DEVANAGARI -> R.string.ocr_script_devanagari
        OcrScript.JAPANESE -> R.string.ocr_script_japanese
        OcrScript.KOREAN -> R.string.ocr_script_korean
    },
)
