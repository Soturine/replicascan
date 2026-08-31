package com.soturine.replicascan.app

import android.content.Context
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.ExportRepository
import com.soturine.replicascan.core.common.repository.OcrRepository
import com.soturine.replicascan.core.common.repository.ScanRepository
import com.soturine.replicascan.core.common.repository.UserPreferencesRepository
import com.soturine.replicascan.core.data.datastore.DefaultUserPreferencesRepository
import com.soturine.replicascan.core.data.export.DefaultExportRepository
import com.soturine.replicascan.core.data.files.ScanFileStore
import com.soturine.replicascan.core.data.image.DefaultDocumentProcessingRepository
import com.soturine.replicascan.core.data.local.ReplicaScanDatabase
import com.soturine.replicascan.core.data.local.ReplicaScanDatabaseFactory
import com.soturine.replicascan.core.data.ocr.DefaultOcrRepository
import com.soturine.replicascan.core.data.repository.DefaultScanRepository

class AppContainer(
    private val context: Context,
) {
    private val database: ReplicaScanDatabase by lazy {
        ReplicaScanDatabaseFactory.create(context)
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

