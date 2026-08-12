package com.soturine.scanora.app

import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.data.files.SourceFileStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class DraftSource(
    val titlePrefix: String,
) {
    QUICK_SCAN("Scan rápido"),
    MANUAL_CAMERA("Modo manual"),
    MANUAL_IMPORT("Importação manual"),
}

sealed interface DraftCreationResult {
    data class Success(
        val scanId: String,
        val firstPageId: String,
        val importedCount: Int,
        val failureCount: Int,
    ) : DraftCreationResult

    data class Failure(
        val requestedCount: Int,
        val importFailureCount: Int,
    ) : DraftCreationResult
}

class ScanDraftCoordinator(
    private val scanRepository: ScanRepository,
    private val fileStore: SourceFileStore,
) {
    suspend fun createDraft(
        mode: ScanMode,
        uriValues: List<String>,
        source: DraftSource,
    ): DraftCreationResult {
        if (uriValues.isEmpty()) return DraftCreationResult.Failure(0, 0)
        val importResult = fileStore.importSources(uriValues)
        if (importResult.imported.isEmpty()) {
            return DraftCreationResult.Failure(uriValues.size, importResult.failures.size)
        }

        return try {
            val formatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
            val created = scanRepository.createScan(
                title = "${source.titlePrefix} ${formatter.format(Date())}",
                mode = mode,
                sourceUris = importResult.imported.map { it.stableUri },
            )
            DraftCreationResult.Success(
                scanId = created.scanId,
                firstPageId = created.pageIds.first(),
                importedCount = importResult.imported.size,
                failureCount = importResult.failures.size,
            )
        } catch (exception: CancellationException) {
            withContext(NonCancellable) {
                fileStore.rollback(importResult.imported)
            }
            throw exception
        } catch (_: Exception) {
            fileStore.rollback(importResult.imported)
            DraftCreationResult.Failure(uriValues.size, importResult.failures.size)
        }
    }
}
