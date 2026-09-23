package com.soturine.replicascan.feature.home

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.util.DateFormatter
import com.soturine.replicascan.core.ui.component.DocumentListItem
import com.soturine.replicascan.core.ui.component.EmptyStateCard
import com.soturine.replicascan.core.ui.component.ReplicaScanContent
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
            if (!state.isLoading && state.recentScans.isEmpty()) {
                item(key = "empty") {
                    ReplicaScanContent {
                        EmptyStateCard(
                            title = stringResource(R.string.home_empty_title),
                            message = stringResource(R.string.home_empty_message),
                            mascotState = ReplicaScanMascotState.Empty,
                        )
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(ReplicaScanSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ReplicaScanSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.home_hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.home_hero_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Image(
                    painter = painterResource(com.soturine.replicascan.core.ui.R.drawable.replicascan_mascot_welcome),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                )
            }
            ReplicaScanPrimaryButton(
                text = stringResource(R.string.home_scan_action),
                onClick = onScan,
                enabled = enabled,
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
