package com.soturine.replicascan.feature.home

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.util.DateFormatter
import com.soturine.replicascan.core.ui.component.DocumentListItem
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
import com.soturine.replicascan.core.ui.component.ReplicaScanMascot
import com.soturine.replicascan.core.ui.component.ReplicaScanMascotState
import com.soturine.replicascan.core.ui.component.ReplicaScanPrimaryButton
import com.soturine.replicascan.core.ui.component.ReplicaScanSecondaryButton
import com.soturine.replicascan.core.ui.theme.ReplicaScanSpacing
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

/** Temporary file handed to the system camera; [path] is what the app later imports. */
data class CaptureTarget(val path: String, val uri: Uri)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    isCreatingDocument: Boolean,
    onScannedPages: (List<String>) -> Unit,
    onImportImages: (List<String>) -> Unit,
    onCapturedPhoto: (String) -> Unit,
    onDiscardCapture: (String) -> Unit,
    newCaptureTarget: () -> CaptureTarget?,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormatter(locale, TimeZone.getDefault()) }
    val cameraUnavailableMessage = stringResource(R.string.home_camera_unavailable)
    var showScannerFallback by rememberSaveable { mutableStateOf(false) }
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_IMPORTED_PAGES),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) onImportImages(uris.map(Uri::toString))
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingCapturePath ?: return@rememberLauncherForActivityResult
        pendingCapturePath = null
        if (saved) onCapturedPhoto(path) else onDiscardCapture(path)
    }
    val launchScanner = rememberDocumentScanner(
        onPages = onScannedPages,
        onUnavailable = { showScannerFallback = true },
    )

    fun importImages() {
        importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun takePhoto() {
        val target = newCaptureTarget() ?: return
        pendingCapturePath = target.path
        try {
            cameraLauncher.launch(target.uri)
        } catch (_: ActivityNotFoundException) {
            pendingCapturePath = null
            onDiscardCapture(target.path)
            coroutineScope.launch { snackbarHostState.showSnackbar(cameraUnavailableMessage) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_app_bar_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, stringResource(R.string.home_history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.home_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = ReplicaScanSpacing.lg, vertical = ReplicaScanSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.sm),
        ) {
            item(key = "hero") {
                ReplicaScanContent {
                    ScanHero(
                        enabled = !isCreatingDocument,
                        onScan = launchScanner,
                        onImport = ::importImages,
                    )
                    AnimatedVisibility(visible = isCreatingDocument) {
                        Column(
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.sm),
                        ) {
                            ReplicaScanMascot(ReplicaScanMascotState.Processing, size = 112.dp)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = stringResource(R.string.home_preparing_pages),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item(key = "recent-header") {
                ReplicaScanContent {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = ReplicaScanSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_recent_section),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        if (state.recentScans.isNotEmpty()) {
                            TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_recent_open_all)) }
                        }
                    }
                }
            }
            if (state.isLoading) {
                item(key = "recent-loading") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = ReplicaScanSpacing.md))
                }
            }
            if (!state.isLoading && state.recentScans.isEmpty()) {
                item(key = "empty") {
                    ReplicaScanContent {
                        HomeEmptyLibrary()
                    }
                }
            }
            items(state.recentScans, key = { it.id }) { scan ->
                ReplicaScanContent(modifier = Modifier.animateItem()) {
                    RecentDocument(scan, dateFormatter, onClick = { onOpenScan(scan.id) })
                }
            }
        }
    }

    if (showScannerFallback) {
        ScannerFallbackDialog(
            onTakePhoto = {
                showScannerFallback = false
                takePhoto()
            },
            onImport = {
                showScannerFallback = false
                importImages()
            },
            onDismiss = { showScannerFallback = false },
        )
    }
}

@Composable
private fun ScanHero(
    enabled: Boolean,
    onScan: () -> Unit,
    onImport: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.md),
    ) {
        if (largeText) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                ReplicaScanMascot(ReplicaScanMascotState.Welcome, size = 72.dp)
            }
        } else {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            HomePaperScene()
        }
        Text(
            text = stringResource(R.string.home_hero_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ReplicaScanPrimaryButton(
            text = stringResource(R.string.home_scan_action),
            onClick = onScan,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 68.dp),
            icon = {
                Icon(Icons.Outlined.DocumentScanner, null)
                Spacer(Modifier.width(10.dp))
            },
        )
        ReplicaScanSecondaryButton(
            text = stringResource(R.string.home_import_action),
            onClick = onImport,
            enabled = enabled,
            outlined = true,
            modifier = Modifier.fillMaxWidth(),
            icon = {
                Icon(Icons.Outlined.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
            },
        )
    }
}

@Composable
private fun HomePaperScene() {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < .5f
    val paperColor = if (isDark) colors.inverseSurface else colors.surface
    val paperInk = if (isDark) colors.inverseOnSurface else colors.onSurface
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier.fillMaxWidth().height(194.dp)
            .clip(shape).background(colors.surfaceContainerHigh),
    ) {
        val glow = colors.primaryContainer.copy(alpha = .60f)
        Canvas(Modifier.matchParentSize()) {
            drawCircle(glow, radius = size.width * .47f, center = Offset(size.width * .78f, size.height * .17f))
            drawLine(colors.outlineVariant, Offset(0f, size.height * .89f),
                Offset(size.width, size.height * .89f), strokeWidth = 2.dp.toPx())
        }
        val pageShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 34.dp)
                .size(width = 154.dp, height = 148.dp)
                .graphicsLayer { rotationZ = -8f }
                .shadow(10.dp, pageShape)
                .background(paperColor, pageShape)
                .border(1.dp, paperInk.copy(alpha = .15f), pageShape)
                .padding(18.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val ink = colors.secondary.copy(alpha = .55f)
                val rule = paperInk.copy(alpha = .25f)
                drawRoundRect(ink, topLeft = Offset(0f, size.height * .04f),
                    size = androidx.compose.ui.geometry.Size(size.width * .43f, 5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                listOf(.28f, .43f, .58f, .73f).forEachIndexed { index, y ->
                    drawLine(rule, Offset(0f, size.height * y),
                        Offset(size.width * if (index == 3) .62f else 1f, size.height * y),
                        strokeWidth = 2.dp.toPx())
                }
                drawRoundRect(colors.secondary.copy(alpha = .60f),
                    topLeft = Offset(size.width * .68f, size.height * .80f),
                    size = androidx.compose.ui.geometry.Size(size.width * .32f, size.height * .18f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()))
            }
        }
        ReplicaScanMascot(
            state = ReplicaScanMascotState.Welcome,
            size = 178.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-10).dp),
        )
    }
}

@Composable
private fun HomeEmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = ReplicaScanSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.home_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentDocument(scan: ScanDocument, formatter: DateFormatter, onClick: () -> Unit) {
    DocumentListItem(
        title = scan.title,
        supportingText = pluralStringResource(R.plurals.home_recent_pages, scan.pageCount, scan.pageCount) +
            " · " + formatter.format(scan.updatedAt),
        imageUri = scan.coverPage?.displayUri,
        fallbackImageUri = scan.coverPage?.sourceUri,
        onClick = onClick,
        trailing = if (scan.isFavorite) {
            {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.home_favorite),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ScannerFallbackDialog(
    onTakePhoto: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DocumentScanner, contentDescription = null) },
        title = { Text(stringResource(R.string.home_scanner_unavailable_title)) },
        text = { Text(stringResource(R.string.home_scanner_unavailable_message)) },
        confirmButton = {
            TextButton(onClick = onTakePhoto) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_take_photo))
            }
        },
        dismissButton = {
            TextButton(onClick = onImport) { Text(stringResource(R.string.home_import_action)) }
        },
    )
}

private const val MAX_IMPORTED_PAGES = 20
