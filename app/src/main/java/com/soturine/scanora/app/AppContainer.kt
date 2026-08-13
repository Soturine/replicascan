package com.soturine.scanora.app

import android.content.Context
import com.soturine.scanora.core.common.repository.DocumentProcessingRepository
import com.soturine.scanora.core.common.repository.ExportRepository
import com.soturine.scanora.core.common.repository.OcrRepository
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.common.repository.UserPreferencesRepository
import com.soturine.scanora.core.data.datastore.DefaultUserPreferencesRepository
import com.soturine.scanora.core.data.export.DefaultExportRepository
import com.soturine.scanora.core.data.files.ScanFileStore
import com.soturine.scanora.core.data.image.DefaultDocumentProcessingRepository
import com.soturine.scanora.core.data.local.ScanoraDatabase
import com.soturine.scanora.core.data.local.ScanoraDatabaseFactory
import com.soturine.scanora.core.data.ocr.DefaultOcrRepository
import com.soturine.scanora.core.data.repository.DefaultScanRepository

class AppContainer(
    private val context: Context,
) {
    private val database: ScanoraDatabase by lazy {
        ScanoraDatabaseFactory.create(context)
    }

    val scanFileStore: ScanFileStore by lazy {
        ScanFileStore(context)
    }

    val scanRepository: ScanRepository by lazy {
        DefaultScanRepository(
            scanDao = database.scanDao(),
            fileStore = scanFileStore,
        )
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DefaultUserPreferencesRepository(context)
    }

    val documentProcessingRepository: DocumentProcessingRepository by lazy {
        DefaultDocumentProcessingRepository(context)
    }

    val scanDraftCoordinator: ScanDraftCoordinator by lazy {
        ScanDraftCoordinator(
            scanRepository = scanRepository,
            fileStore = scanFileStore,
        )
    }

    val exportRepository: ExportRepository by lazy {
        DefaultExportRepository(
            context = context,
            processingRepository = documentProcessingRepository,
        )
    }

    val ocrRepository: OcrRepository by lazy {
        DefaultOcrRepository(context)
    }
}

