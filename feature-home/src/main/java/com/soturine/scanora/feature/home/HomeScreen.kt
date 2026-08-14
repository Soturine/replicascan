package com.soturine.scanora.feature.home

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.util.DateFormatter
import com.soturine.scanora.core.ui.component.AsyncUriImage
import com.soturine.scanora.core.ui.component.EmptyStateCard
import com.soturine.scanora.core.ui.component.ScanoraMascotState
import com.soturine.scanora.core.ui.component.ScanoraContent
import com.soturine.scanora.core.ui.component.ScanoraPrimaryButton
import com.soturine.scanora.core.ui.component.ScanoraSecondaryButton
import com.soturine.scanora.core.ui.theme.ScanoraSpacing
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartQuickScan: (List<String>) -> Unit,
    onOpenManualCamera: (ScanMode) -> Unit,
    onImportImages: (ScanMode, List<String>) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScan: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    val dateFormatter = remember(locale) { DateFormatter(locale, TimeZone.getDefault()) }
    val quickScanUnavailableMessage = stringResource(R.string.home_quick_scan_unavailable)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) onImportImages(state.manualMode, uris.map(Uri::toString))
    }
    val guidedScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            ?.pages.orEmpty().map { it.imageUri.toString() }
        if (pages.isNotEmpty()) onStartQuickScan(pages)
    }

    fun launchQuickScan() {
        if (activity == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(quickScanUnavailableMessage) }
            return
        }
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(12)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { guidedScanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener {
                coroutineScope.launch { snackbarHostState.showSnackbar(quickScanUnavailableMessage) }
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_app_bar_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
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
            contentPadding = PaddingValues(horizontal = ScanoraSpacing.xl, vertical = ScanoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ScanoraSpacing.xl),
        ) {
            item {
                ScanoraContent {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
                                        ),
                                    ),
                                ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        stringResource(R.string.home_greeting),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(stringResource(R.string.home_welcome), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(R.string.home_start_hint), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Image(
                                    painter = painterResource(com.soturine.scanora.core.ui.R.drawable.scanora_mascot_welcome),
                                    contentDescription = null,
                                    modifier = Modifier.size(150.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                    ScanoraPrimaryButton(
                        text = stringResource(R.string.home_quick_scan_action),
                        onClick = ::launchQuickScan,
                        icon = {
                            Icon(Icons.Outlined.DocumentScanner, null)
                            Spacer(Modifier.width(10.dp))
                        },
                    )
    ScanoraSecondaryButton(
                        text = stringResource(R.string.home_manual_scan_action),
                        onClick = { onOpenManualCamera(state.manualMode) },
                        modifier = Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(10.dp)) },
                    )
                    ScanoraSecondaryButton(
                        text = stringResource(R.string.home_import_gallery_action),
                        onClick = {
                            importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        outlined = true,
                        icon = { Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(10.dp)) },
                    )
                }
            }
            item {
                ScanoraContent {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.home_recent_section), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_recent_open_all)) }
                    }
                    if (state.recentScans.isEmpty()) {
                        EmptyStateCard(
                            title = stringResource(R.string.home_empty_title),
                            message = stringResource(R.string.home_empty_message),
                            mascotState = ScanoraMascotState.Empty,
                        )
                    }
                }
            }
            items(state.recentScans, key = { it.id }) { scan ->
                ScanoraContent {
                    HomeRecentCard(scan, dateFormatter, onClick = { onOpenScan(scan.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRecentCard(scan: ScanDocument, formatter: DateFormatter, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncUriImage(
                imageUri = scan.coverPage?.displayUri,
                fallbackImageUri = scan.coverPage?.sourceUri,
                modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.small),
                maxDimension = 600,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(scan.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.home_recent_pages, scan.pageCount, scan.pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatter.format(scan.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
