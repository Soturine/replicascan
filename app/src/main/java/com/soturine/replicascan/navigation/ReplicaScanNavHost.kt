package com.soturine.replicascan.navigation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soturine.replicascan.BuildConfig
import com.soturine.replicascan.R
import com.soturine.replicascan.app.AppContainer
import com.soturine.replicascan.app.DraftCreationResult
import com.soturine.replicascan.app.RootViewModel
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.feature.editor.CropScreen
import com.soturine.replicascan.feature.editor.EditorViewModel
import com.soturine.replicascan.feature.editor.FilterScreen
import com.soturine.replicascan.feature.editor.ReviewScreen
import com.soturine.replicascan.feature.export.ExportScreen
import com.soturine.replicascan.feature.export.ExportViewModel
import com.soturine.replicascan.feature.history.HistoryScreen
import com.soturine.replicascan.feature.history.HistoryViewModel
import com.soturine.replicascan.feature.history.ScanDetailScreen
import com.soturine.replicascan.feature.history.ScanDetailViewModel
import com.soturine.replicascan.feature.home.CaptureTarget
import com.soturine.replicascan.feature.home.HomeScreen
import com.soturine.replicascan.feature.home.HomeViewModel
import com.soturine.replicascan.feature.ocr.OcrScreen
import com.soturine.replicascan.feature.ocr.OcrViewModel
import com.soturine.replicascan.feature.settings.AboutScreen
import com.soturine.replicascan.feature.settings.SettingsScreen
import com.soturine.replicascan.feature.settings.SettingsViewModel
import com.soturine.replicascan.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://github.com/Soturine/replicascan/blob/main/PRIVACY_POLICY.md"

@Composable
fun ReplicaScanNavHost(
    container: AppContainer,
    rootViewModel: RootViewModel,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val rootState = rootViewModel.uiState.collectAsStateWithLifecycle()
    // One draft at a time: repeated taps or a second scanner result cannot create duplicate documents.
    var creatingDocument by remember { mutableStateOf(false) }
    val startDestination = if (rootState.value.onboardingCompleted) {
        ReplicaScanDestinations.Home
    } else {
        ReplicaScanDestinations.Onboarding
    }

    fun createDocument(uris: List<String>, titleRes: Int, openCrop: Boolean) {
        if (creatingDocument) return
        creatingDocument = true
        coroutineScope.launch {
            try {
                when (val result = container.scanDraftCoordinator.createDraft(uris, context.getString(titleRes))) {
                    is DraftCreationResult.Success -> {
                        if (result.failureCount > 0) {
                            val total = result.importedCount + result.failureCount
                            context.toast(context.getString(R.string.import_partial_result, result.importedCount, total, result.failureCount))
                        }
                        navController.navigate(ReplicaScanDestinations.review(result.scanId))
                        if (openCrop) navController.navigate(ReplicaScanDestinations.crop(result.scanId, result.firstPageId))
                    }
                    is DraftCreationResult.Failure -> if (result.requestedCount > 0) {
                        context.toast(context.getString(R.string.import_failed_result))
                    }
                }
            } finally {
                creatingDocument = false
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ReplicaScanDestinations.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    rootViewModel.completeOnboarding()
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(ReplicaScanDestinations.Home) {
                            popUpTo(ReplicaScanDestinations.Onboarding) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(ReplicaScanDestinations.Home) {
            val homeViewModel: HomeViewModel = featureViewModel { HomeViewModel(container.scanRepository) }
            val state = homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                state = state.value,
                isCreatingDocument = creatingDocument,
                onScannedPages = { uris -> createDocument(uris, R.string.draft_title_scan, openCrop = false) },
                onImportImages = { uris -> createDocument(uris, R.string.draft_title_import, openCrop = false) },
                onCapturedPhoto = { path -> createDocument(listOf(path), R.string.draft_title_photo, openCrop = true) },
                onDiscardCapture = container.scanFileStore::discardCapture,
                newCaptureTarget = {
                    runCatching {
                        val file = container.scanFileStore.newCaptureFile()
                        CaptureTarget(file.absolutePath, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                    }.getOrNull()
                },
                onOpenHistory = { navController.navigate(ReplicaScanDestinations.History) },
                onOpenSettings = { navController.navigate(ReplicaScanDestinations.Settings) },
                onOpenScan = { scanId -> navController.navigate(ReplicaScanDestinations.detail(scanId)) },
            )
        }
        composable(ReplicaScanDestinations.Crop, arguments = scanPageArguments) { entry ->
            val (scanId, pageId) = entry.scanAndPage()
            val editorViewModel = editorViewModel(container, "crop-$scanId-$pageId", scanId, pageId)
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            CropScreen(
                state = state.value,
                onDone = { quad -> editorViewModel.updateQuad(quad) { navController.popBackStack() } },
                onBack = { navController.popBackStack() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }
        composable(ReplicaScanDestinations.Filters, arguments = scanPageArguments) { entry ->
            val (scanId, pageId) = entry.scanAndPage()
            val editorViewModel = editorViewModel(container, "filter-$scanId-$pageId", scanId, pageId)
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            FilterScreen(
                state = state.value,
                onRequestPreview = editorViewModel::prepareFilterPreview,
                onApply = { filter -> editorViewModel.applyFilter(filter) { navController.popBackStack() } },
                onBack = { navController.popBackStack() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }
        composable(ReplicaScanDestinations.Review, arguments = listOf(stringArgument("scanId"))) { entry ->
            val scanId = entry.stringArgument("scanId")
            val editorViewModel = editorViewModel(container, "review-$scanId", scanId, null)
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            ReviewScreen(
                state = state.value,
                onPreparePreview = editorViewModel::prepareCurrentPagePreview,
                onSelectPage = editorViewModel::selectPage,
                onMovePage = editorViewModel::movePage,
                onDeleteCurrentPage = editorViewModel::deleteCurrentPage,
                onRename = editorViewModel::renameScan,
                onUpdateTags = editorViewModel::updateTags,
                onRotate = editorViewModel::rotateCurrentPage,
                onOpenCrop = { pageId -> navController.navigate(ReplicaScanDestinations.crop(scanId, pageId)) },
                onOpenFilters = { pageId -> navController.navigate(ReplicaScanDestinations.filters(scanId, pageId)) },
                onOpenOcr = { pageId -> navController.navigate(ReplicaScanDestinations.ocr(scanId, pageId)) },
                onOpenExport = { navController.navigate(ReplicaScanDestinations.export(scanId)) },
                onBack = { navController.popToHome() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }
        composable(ReplicaScanDestinations.History) {
            val historyViewModel: HistoryViewModel = featureViewModel { HistoryViewModel(container.scanRepository) }
            val state = historyViewModel.uiState.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state.value,
                onQueryChange = historyViewModel::onQueryChange,
                onOpenScan = { scanId -> navController.navigate(ReplicaScanDestinations.detail(scanId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ReplicaScanDestinations.Detail, arguments = listOf(stringArgument("scanId"))) { entry ->
            val scanId = entry.stringArgument("scanId")
            val detailViewModel: ScanDetailViewModel = featureViewModel(key = "detail-$scanId") {
                ScanDetailViewModel(scanId = scanId, scanRepository = container.scanRepository)
            }
            val state = detailViewModel.state.collectAsStateWithLifecycle()
            ScanDetailScreen(
                scan = state.value?.scan,
                isLoaded = state.value != null,
                onToggleFavorite = detailViewModel::toggleFavorite,
                onDeleteScan = {
                    detailViewModel.deleteScan { outcome ->
                        if (outcome.hasCleanupFailures) {
                            context.toast(context.getString(R.string.delete_partial_result, outcome.failedFileCount))
                        }
                        navController.popBackStack()
                    }
                },
                onOpenReview = { navController.navigate(ReplicaScanDestinations.review(scanId)) },
                onOpenExport = { navController.navigate(ReplicaScanDestinations.export(scanId)) },
                onOpenOcr = { pageId -> navController.navigate(ReplicaScanDestinations.ocr(scanId, pageId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ReplicaScanDestinations.Export, arguments = listOf(stringArgument("scanId"))) { entry ->
            val scanId = entry.stringArgument("scanId")
            val exportViewModel: ExportViewModel = featureViewModel(key = "export-$scanId") {
                ExportViewModel(
                    scanId = scanId,
                    scanRepository = container.scanRepository,
                    preferencesRepository = container.userPreferencesRepository,
                    exportRepository = container.exportRepository,
                )
            }
            val state = exportViewModel.uiState.collectAsStateWithLifecycle()
            ExportScreen(
                state = state.value,
                onSelectFormat = exportViewModel::selectFormat,
                onSelectQuality = exportViewModel::selectQuality,
                onSelectPageSize = exportViewModel::selectPageSize,
                onExport = exportViewModel::export,
                onShare = { files -> shareFiles(context, files) },
                onOpenFile = { file -> openExportedFile(context, file) },
                onBack = { navController.popBackStack() },
                onClearMessage = exportViewModel::clearMessage,
            )
        }
        composable(ReplicaScanDestinations.Ocr, arguments = scanPageArguments) { entry ->
            val (scanId, pageId) = entry.scanAndPage()
            val ocrViewModel: OcrViewModel = featureViewModel(key = "ocr-$scanId-$pageId") {
                OcrViewModel(
                    scanId = scanId,
                    pageId = pageId,
                    scanRepository = container.scanRepository,
                    processingRepository = container.documentProcessingRepository,
                    ocrRepository = container.ocrRepository,
                )
            }
            val state = ocrViewModel.uiState.collectAsStateWithLifecycle()
            OcrScreen(
                state = state.value,
                onRetry = ocrViewModel::retry,
                onScriptSelected = ocrViewModel::selectScript,
                onBack = { navController.popBackStack() },
                onClearMessage = ocrViewModel::clearMessage,
            )
        }
        composable(ReplicaScanDestinations.Settings) {
            val settingsViewModel: SettingsViewModel = featureViewModel { SettingsViewModel(container.userPreferencesRepository) }
            val state = settingsViewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state.value,
                currentLanguageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                onThemeSelected = settingsViewModel::setTheme,
                onLanguageSelected = { tag -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
                onPdfQualitySelected = settingsViewModel::setPdfQuality,
                onOpenIntroduction = { navController.navigate(ReplicaScanDestinations.Onboarding) },
                onOpenPrivacyPolicy = { openPrivacyPolicy(context) },
                onOpenAbout = { navController.navigate(ReplicaScanDestinations.About) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ReplicaScanDestinations.About) {
            AboutScreen(
                versionName = BuildConfig.VERSION_NAME,
                onOpenPrivacyPolicy = { openPrivacyPolicy(context) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private val scanPageArguments = listOf(stringArgument("scanId"), stringArgument("pageId"))

private fun stringArgument(name: String) = navArgument(name) { type = NavType.StringType }

private fun NavBackStackEntry.stringArgument(name: String): String = arguments?.getString(name).orEmpty()

private fun NavBackStackEntry.scanAndPage(): Pair<String, String> = stringArgument("scanId") to stringArgument("pageId")

/** Review is reached right after scanning; “back” returns to wherever the document came from, or Home. */
private fun NavHostController.popToHome() {
    if (!popBackStack(ReplicaScanDestinations.Detail, inclusive = false) &&
        !popBackStack(ReplicaScanDestinations.Home, inclusive = false)
    ) {
        popBackStack()
    }
}

@Composable
private fun editorViewModel(container: AppContainer, key: String, scanId: String, pageId: String?): EditorViewModel =
    featureViewModel(key = key) {
        EditorViewModel(
            scanId = scanId,
            initialPageId = pageId,
            scanRepository = container.scanRepository,
            processingRepository = container.documentProcessingRepository,
        )
    }

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private fun openPrivacyPolicy(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
    } catch (_: ActivityNotFoundException) {
        context.toast(context.getString(R.string.no_app_to_open))
    }
}

private fun shareFiles(context: Context, files: List<ExportedFile>) {
    if (files.isEmpty()) return
    val uris = files.map { Uri.parse(it.uri) }
    val clip = ClipData.newUri(context.contentResolver, files.first().displayName, uris.first()).apply {
        uris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }.apply {
        type = files.first().mimeType
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_chooser_title)))
    } catch (_: ActivityNotFoundException) {
        context.toast(context.getString(R.string.no_app_to_open))
    }
}

private fun openExportedFile(context: Context, file: ExportedFile) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(file.uri), file.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        shareFiles(context, listOf(file))
    }
}

@Composable
private inline fun <reified T : ViewModel> featureViewModel(
    key: String? = null,
    crossinline create: () -> T,
): T {
    val factory = remember(key) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
        }
    }
    return if (key == null) viewModel(factory = factory) else viewModel(key = key, factory = factory)
}
