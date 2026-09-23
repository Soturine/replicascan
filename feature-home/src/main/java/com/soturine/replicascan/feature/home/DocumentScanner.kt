package com.soturine.replicascan.feature.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * ReplicaScan's only entry point into ML Kit Document Scanner.
 *
 * The provider owns capture, edge detection, perspective correction, cleanup and its own gallery
 * picker; ReplicaScan only receives JPEG pages and immediately copies them into private storage.
 * Cancel or an empty result is a no-op. Any failure to start (no Google Play services, device below
 * the scanner's RAM floor, module still unavailable) is reported through [onUnavailable].
 */
@Composable
fun rememberDocumentScanner(
    onPages: (List<String>) -> Unit,
    onUnavailable: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val currentOnPages by rememberUpdatedState(onPages)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            ?.pages.orEmpty()
            .map { it.imageUri.toString() }
        if (pages.isNotEmpty()) currentOnPages(pages)
    }
    val client = remember { GmsDocumentScanning.getClient(scannerOptions()) }
    return remember(context, launcher, client) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                currentOnUnavailable()
            } else {
                client.getStartScanIntent(activity)
                    .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
                    .addOnFailureListener { currentOnUnavailable() }
            }
        }
    }
}

internal fun scannerOptions(): GmsDocumentScannerOptions =
    GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setPageLimit(MAX_SCANNED_PAGES)
        // Only JPEG pages are consumed; the PDF is built later from the private sources.
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .build()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val MAX_SCANNED_PAGES = 20
