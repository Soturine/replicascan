package com.soturine.scanora.app

import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.repository.ScanRepository
import com.soturine.scanora.core.data.files.SourceFileStore
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class DraftSource {
    QUICK_SCAN,
    MANUAL_CAMERA,
    MANUAL_IMPORT,
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
        titlePrefix: String,
    ): DraftCreationResult {
        if (uriValues.isEmpty()) return DraftCreationResult.Failure(0, 0)
        val importResult = fileStore.importSources(uriValues)
        if (importResult.imported.isEmpty()) {
            return DraftCreationResult.Failure(uriValues.size, importResult.failures.size)
        }

        return try {
            val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            val created = scanRepository.createScan(
                title = "$titlePrefix ${formatter.format(Date())}",
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
