package com.soturine.replicascan.feature.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.image.DocumentQuadValidator
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import com.soturine.replicascan.core.ui.component.AsyncUriImage
import com.soturine.replicascan.core.ui.component.ReplicaScanToolButton

/**
 * Manual crop: the always-available escape hatch. Pages from ML Kit are already corrected, so the
 * default is the full page; nothing here guesses edges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    state: EditorUiState,
    onDone: (DocumentQuad) -> Unit,
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
    val savedQuad = page.quad ?: DocumentQuad.FULL_PAGE
    var localQuad by rememberSaveable(page.id, savedQuad, stateSaver = QuadSaver) { mutableStateOf(savedQuad) }
    var touched by rememberSaveable(page.id) { mutableStateOf(false) }
    val isQuadValid = DocumentQuadValidator.isValidNormalized(localQuad)

    EditorMessageEffect(state.message, snackbarHostState, onClearMessage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_crop_title)) },
                navigationIcon = { EditorBackButton(onBack) },
                actions = {
                    TextButton(
                        onClick = { onDone(localQuad) },
                        enabled = isQuadValid && !state.isProcessing,
                    ) { Text(stringResource(R.string.editor_done)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            DocumentCropPreview(
                imageUri = page.sourceUri,
                quad = localQuad,
                onQuadChange = {
                    localQuad = it
                    touched = true
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(20.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = if (!isQuadValid) CropHint.INVALID else if (touched) CropHint.NONE else CropHint.COACH,
                    label = "cropHint",
                ) { hint ->
                    Text(
                        text = when (hint) {
                            CropHint.INVALID -> stringResource(R.string.editor_crop_invalid)
                            CropHint.COACH -> stringResource(R.string.editor_crop_hint)
                            CropHint.NONE -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hint == CropHint.INVALID) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    ReplicaScanToolButton(
                        icon = Icons.Outlined.CropFree,
                        label = stringResource(R.string.editor_crop_full_page),
                        onClick = {
                            localQuad = DocumentQuad.FULL_PAGE
                            touched = true
                        },
                        enabled = !state.isProcessing && localQuad != DocumentQuad.FULL_PAGE,
                    )
                    ReplicaScanToolButton(
                        icon = Icons.AutoMirrored.Outlined.Undo,
                        label = stringResource(R.string.editor_crop_reset),
                        onClick = { localQuad = savedQuad },
                        enabled = !state.isProcessing && localQuad != savedQuad,
                    )
                }
            }
        }
    }
}

private enum class CropHint { COACH, INVALID, NONE }

private val QuadSaver = Saver<DocumentQuad, List<Float>>(
    save = { quad -> quad.asList().flatMap { listOf(it.x, it.y) } },
    restore = { values ->
        DocumentQuad(
            topLeft = PointValue(values[0], values[1]),
            topRight = PointValue(values[2], values[3]),
            bottomRight = PointValue(values[4], values[5]),
            bottomLeft = PointValue(values[6], values[7]),
        )
    },
)

@Composable
private fun DocumentCropPreview(
    imageUri: String,
    quad: DocumentQuad,
    onQuadChange: (DocumentQuad) -> Unit,
    modifier: Modifier = Modifier,
) {
    var imageSize by remember(imageUri) { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(modifier = modifier) {
        val imageBounds = remember(constraints.maxWidth, constraints.maxHeight, imageSize) {
            computeFittedBounds(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), imageSize)
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

private fun computeFittedBounds(containerWidth: Float, containerHeight: Float, imageSize: IntSize): Rect? {
    if (containerWidth <= 0f || containerHeight <= 0f || imageSize.width == 0 || imageSize.height == 0) return null
    val imageAspect = imageSize.width.toFloat() / imageSize.height
    val containerAspect = containerWidth / containerHeight
    return if (imageAspect > containerAspect) {
        val fittedHeight = containerWidth / imageAspect
        val top = (containerHeight - fittedHeight) / 2f
        Rect(0f, top, containerWidth, top + fittedHeight)
    } else {
        val fittedWidth = containerHeight * imageAspect
        val left = (containerWidth - fittedWidth) / 2f
        Rect(left, 0f, left + fittedWidth, containerHeight)
    }
}
