package com.soturine.replicascan.feature.editor

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.ui.component.AsyncUriImage
import com.soturine.replicascan.core.ui.localizedTitle
import kotlin.math.max

/** Optional look for one page. Selecting only previews; “Apply” persists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    state: EditorUiState,
    onRequestPreview: (DocumentFilterType, Int) -> Unit,
    onApply: (DocumentFilterType) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = state.currentPage
    if (page == null) {
        if (state.isLoaded) MissingDocument(onBack, modifier)
        return
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var selected by rememberSaveable(page.id, page.filterType) { mutableStateOf(page.filterType) }
    var comparing by remember(page.id) { mutableStateOf(false) }
    var previewLongSide by remember(page.id) { mutableIntStateOf(1_400) }
    val previewFilter = if (comparing) DocumentFilterType.ORIGINAL else selected

    EditorMessageEffect(state.message, snackbarHostState, onClearMessage)
    LaunchedEffect(page.id, page.quad, page.rotationDegrees, previewFilter, previewLongSide) {
        onRequestPreview(previewFilter, previewLongSide)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_filter_title)) },
                navigationIcon = { EditorBackButton(onBack) },
                actions = {
                    TextButton(onClick = { onApply(selected) }, enabled = !state.isProcessing) {
                        Text(stringResource(R.string.editor_apply))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isPreviewLoading || state.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp)
                    .onSizeChanged { size -> previewLongSide = max(size.width, size.height) },
            ) {
                Crossfade(targetState = state.previewImageUri, label = "lookPreview") { uri ->
                    AsyncUriImage(
                        imageUri = uri,
                        fallbackImageUri = page.sourceUri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        maxDimension = previewLongSide.coerceIn(1_200, 1_800),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(DocumentFilterType.entries, key = { it.storageKey }) { filter ->
                        FilterChip(
                            selected = selected == filter,
                            onClick = {
                                comparing = false
                                selected = filter
                            },
                            label = { Text(filter.localizedTitle()) },
                            leadingIcon = if (selected == filter) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
                            } else {
                                null
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                if (selected != DocumentFilterType.ORIGINAL) {
                    FilterChip(
                        selected = comparing,
                        onClick = { comparing = !comparing },
                        label = { Text(stringResource(R.string.editor_filter_compare)) },
                        leadingIcon = { Icon(Icons.Outlined.Compare, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}
