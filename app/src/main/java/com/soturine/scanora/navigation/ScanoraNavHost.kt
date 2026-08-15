package com.soturine.scanora.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soturine.scanora.app.AppContainer
import com.soturine.scanora.app.DraftCreationResult
import com.soturine.scanora.app.DraftSource
import com.soturine.scanora.app.RootViewModel
import com.soturine.scanora.R
import com.soturine.scanora.core.common.model.ExportedFile
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.feature.camera.CameraCaptureScreen
import com.soturine.scanora.feature.camera.CameraCaptureViewModel
import com.soturine.scanora.feature.editor.CropScreen
import com.soturine.scanora.feature.editor.EditorViewModel
import com.soturine.scanora.feature.editor.FilterScreen
import com.soturine.scanora.feature.editor.ReviewScreen
import com.soturine.scanora.feature.export.ExportScreen
import com.soturine.scanora.feature.export.ExportViewModel
import com.soturine.scanora.feature.history.HistoryScreen
import com.soturine.scanora.feature.history.HistoryViewModel
import com.soturine.scanora.feature.history.ScanDetailScreen
import com.soturine.scanora.feature.history.ScanDetailViewModel
import com.soturine.scanora.feature.home.HomeScreen
import com.soturine.scanora.feature.home.HomeViewModel
import com.soturine.scanora.feature.ocr.OcrScreen
import com.soturine.scanora.feature.ocr.OcrViewModel
import com.soturine.scanora.feature.settings.AboutScreen
import com.soturine.scanora.feature.settings.SettingsScreen
import com.soturine.scanora.feature.settings.SettingsViewModel
import com.soturine.scanora.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun ScanoraNavHost(
    container: AppContainer,
    rootViewModel: RootViewModel,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val rootState = rootViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (rootState.value.onboardingCompleted) {
        ScanoraDestinations.Home
    } else {
        ScanoraDestinations.Onboarding
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(ScanoraDestinations.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    rootViewModel.completeOnboarding()
                    navController.navigate(ScanoraDestinations.Home) {
                        popUpTo(ScanoraDestinations.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(ScanoraDestinations.Home) {
            val homeViewModel: HomeViewModel = featureViewModel {
                HomeViewModel(
                    scanRepository = container.scanRepository,
                    preferencesRepository = container.userPreferencesRepository,
                )
            }
            val state = homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                state = state.value,
                onStartQuickScan = { uris ->
                    coroutineScope.launch {
                        container.scanDraftCoordinator.createDraft(
                            mode = ScanMode.DOCUMENT,
                            uriValues = uris,
                            source = DraftSource.QUICK_SCAN,
                            titlePrefix = resources.getString(R.string.draft_title_quick),
                        ).handle(
                            context = context,
                            onSuccess = { result ->
                                navController.navigate(ScanoraDestinations.review(result.scanId))
                            },
                        )
                    }
                },
                onOpenManualCamera = { mode ->
                    navController.navigate(ScanoraDestinations.camera(mode))
                },
                onImportImages = { mode, uris ->
                    coroutineScope.launch {
                        container.scanDraftCoordinator.createDraft(
                            mode = mode,
                            uriValues = uris,
                            source = DraftSource.MANUAL_IMPORT,
                            titlePrefix = resources.getString(R.string.draft_title_import),
                        ).handle(context) { result ->
                            navController.navigate(ScanoraDestinations.crop(result.scanId, result.firstPageId))
                        }
                    }
                },
                onOpenHistory = { navController.navigate(ScanoraDestinations.History) },
                onOpenSettings = { navController.navigate(ScanoraDestinations.Settings) },
                onOpenScan = { scanId ->
                    navController.navigate(ScanoraDestinations.detail(scanId))
                },
            )
        }
        composable(
            route = ScanoraDestinations.Camera,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = ScanMode.fromStorageKey(entry.arguments?.getString("mode").orEmpty())
            val cameraViewModel: CameraCaptureViewModel = featureViewModel(key = "camera-${mode.storageKey}") {
                CameraCaptureViewModel(mode, container.documentProcessingRepository)
            }
            val state = cameraViewModel.uiState.collectAsStateWithLifecycle()
            CameraCaptureScreen(
                state = state.value,
                onPermissionResult = cameraViewModel::onPermissionResult,
                onCapturedImage = cameraViewModel::onCaptured,
                onAnalyzeFrame = cameraViewModel::analyzeFrame,
                onDone = { capturedUris ->
                    coroutineScope.launch {
                        container.scanDraftCoordinator.createDraft(
                            mode = mode,
                            uriValues = capturedUris,
                            source = DraftSource.MANUAL_CAMERA,
                            titlePrefix = resources.getString(R.string.draft_title_camera),
                        ).handle(context) { result ->
                            navController.navigate(ScanoraDestinations.crop(result.scanId, result.firstPageId))
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onCaptureStarted = cameraViewModel::tryStartCapture,
                onCaptureFinished = cameraViewModel::onCaptureFinished,
                onError = cameraViewModel::onError,
            )
        }
        composable(
            route = ScanoraDestinations.Crop,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType },
            ),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val pageId = entry.arguments?.getString("pageId").orEmpty()
            val editorViewModel: EditorViewModel = featureViewModel(key = "crop-$scanId-$pageId") {
                EditorViewModel(
                    scanId = scanId,
                    initialPageId = pageId,
                    scanRepository = container.scanRepository,
                    processingRepository = container.documentProcessingRepository,
                )
            }
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            CropScreen(
                state = state.value,
                onSaveQuadAndContinue = { quad ->
                    editorViewModel.updateQuad(quad) {
                        navController.navigate(ScanoraDestinations.filters(scanId, pageId))
                    }
                },
                onEnsureQuad = editorViewModel::ensureQuadForCurrentPage,
                onReestimate = editorViewModel::reestimateCurrentPageQuad,
                onBack = { navController.popBackStack() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }
        composable(
            route = ScanoraDestinations.Filters,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType },
            ),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val pageId = entry.arguments?.getString("pageId").orEmpty()
            val editorViewModel: EditorViewModel = featureViewModel(key = "filter-$scanId-$pageId") {
                EditorViewModel(
                    scanId = scanId,
                    initialPageId = pageId,
                    scanRepository = container.scanRepository,
                    processingRepository = container.documentProcessingRepository,
                )
            }
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            FilterScreen(
                state = state.value,
                onApplyFilter = editorViewModel::applyFilter,
                onRequestPreview = editorViewModel::prepareFilterPreview,
                onRotate = editorViewModel::rotateCurrentPage,
                onOpenReview = { navController.navigate(ScanoraDestinations.review(scanId)) },
                onBack = { navController.popBackStack() },
                onClearMessage = editorViewModel::clearMessage,
            )
        }
        composable(
            route = ScanoraDestinations.Review,
            arguments = listOf(navArgument("scanId") { type = NavType.StringType }),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val editorViewModel: EditorViewModel = featureViewModel(key = "review-$scanId") {
                EditorViewModel(
                    scanId = scanId,
                    initialPageId = null,
                    scanRepository = container.scanRepository,
                    processingRepository = container.documentProcessingRepository,
                )
            }
            val state = editorViewModel.uiState.collectAsStateWithLifecycle()
            ReviewScreen(
                state = state.value,
                onRename = editorViewModel::renameScan,
                onUpdateTags = editorViewModel::updateTags,
                onPreparePreview = editorViewModel::prepareCurrentPagePreview,
                onClearMessage = editorViewModel::clearMessage,
                onSelectPage = editorViewModel::selectPage,
                onMovePageUp = { editorViewModel.movePage(it, -1) },
                onMovePageDown = { editorViewModel.movePage(it, 1) },
                onDeleteCurrentPage = editorViewModel::deleteCurrentPage,
                onRotate = editorViewModel::rotateCurrentPage,
                onOpenCrop = {
                    state.value.currentPage?.id?.let { pageId ->
                        navController.navigate(ScanoraDestinations.crop(scanId, pageId))
                    }
                },
                onOpenFilters = {
                    state.value.currentPage?.id?.let { pageId ->
                        navController.navigate(ScanoraDestinations.filters(scanId, pageId))
                    }
                },
                onOpenExport = { navController.navigate(ScanoraDestinations.export(scanId)) },
                onOpenOcr = { pageId ->
                    navController.navigate(ScanoraDestinations.ocr(scanId, pageId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(ScanoraDestinations.History) {
            val historyViewModel: HistoryViewModel = featureViewModel {
                HistoryViewModel(container.scanRepository)
            }
            val state = historyViewModel.uiState.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state.value,
                onQueryChange = historyViewModel::onQueryChange,
                onOpenScan = { scanId ->
                    navController.navigate(ScanoraDestinations.detail(scanId))
                },
            )
        }
        composable(
            route = ScanoraDestinations.Detail,
            arguments = listOf(navArgument("scanId") { type = NavType.StringType }),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val detailViewModel: ScanDetailViewModel = featureViewModel(key = "detail-$scanId") {
                ScanDetailViewModel(
                    scanId = scanId,
                    scanRepository = container.scanRepository,
                )
            }
            val state = detailViewModel.scan.collectAsStateWithLifecycle()
            ScanDetailScreen(
                scan = state.value,
                onToggleFavorite = detailViewModel::toggleFavorite,
                onDeleteScan = {
                    detailViewModel.deleteScan { outcome ->
                        if (outcome.hasCleanupFailures) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.delete_partial_result, outcome.failedFileCount),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        navController.popBackStack()
                    }
                },
                onOpenReview = { navController.navigate(ScanoraDestinations.review(scanId)) },
                onOpenExport = { navController.navigate(ScanoraDestinations.export(scanId)) },
                onOpenOcr = { pageId -> navController.navigate(ScanoraDestinations.ocr(scanId, pageId)) },
            )
        }
        composable(
            route = ScanoraDestinations.Export,
            arguments = listOf(navArgument("scanId") { type = NavType.StringType }),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
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
        composable(
            route = ScanoraDestinations.Ocr,
            arguments = listOf(
                navArgument("scanId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType },
            ),
        ) { entry ->
            val scanId = entry.arguments?.getString("scanId").orEmpty()
            val pageId = entry.arguments?.getString("pageId").orEmpty()
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
                onRecognizeAgain = ocrViewModel::recognize,
                onScriptSelected = ocrViewModel::selectScript,
                onBack = { navController.popBackStack() },
                onClearMessage = ocrViewModel::clearMessage,
            )
        }
        composable(ScanoraDestinations.Settings) {
            val settingsViewModel: SettingsViewModel = featureViewModel {
                SettingsViewModel(container.userPreferencesRepository)
            }
            val state = settingsViewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state.value,
                onThemeSelected = settingsViewModel::setTheme,
                onPdfQualitySelected = settingsViewModel::setPdfQuality,
                onResetOnboarding = settingsViewModel::resetOnboarding,
                onOpenAbout = { navController.navigate(ScanoraDestinations.About) },
                currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                onLanguageSelected = { tag ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                },
            )
        }
        composable(ScanoraDestinations.About) {
            AboutScreen(
                onOpenPrivacyPolicy = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Soturine/scanora/blob/main/PRIVACY_POLICY.md"),
                    )
                    context.startActivity(intent)
                },
            )
        }
    }
}

private fun DraftCreationResult.handle(
    context: Context,
    onSuccess: (DraftCreationResult.Success) -> Unit,
) {
    when (this) {
        is DraftCreationResult.Success -> {
            if (failureCount > 0) {
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.import_partial_result,
                        importedCount,
                        importedCount + failureCount,
                        failureCount,
                    ),
                    Toast.LENGTH_LONG,
                ).show()
            }
            onSuccess(this)
        }

        is DraftCreationResult.Failure -> {
            Toast.makeText(context, R.string.import_failed_result, Toast.LENGTH_LONG).show()
        }
    }
}

private fun shareFiles(
    context: android.content.Context,
    files: List<ExportedFile>,
) {
    if (files.isEmpty()) return
    val uris = files.map { Uri.parse(it.uri) }
    val sharedClipData = android.content.ClipData.newUri(
        context.contentResolver,
        "Scanora export",
        uris.first(),
    ).apply {
        uris.drop(1).forEach { uri -> addItem(android.content.ClipData.Item(uri)) }
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = files.first().mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
            clipData = sharedClipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = files.first().mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = sharedClipData
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_chooser_title)))
}

private fun openExportedFile(
    context: android.content.Context,
    file: ExportedFile,
) {
    val uri = Uri.parse(file.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, file.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val packageManager = context.packageManager
    if (intent.resolveActivity(packageManager) != null) {
        context.startActivity(intent)
    } else {
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
    return if (key == null) {
        viewModel(factory = factory)
    } else {
        viewModel(key = key, factory = factory)
    }
}
