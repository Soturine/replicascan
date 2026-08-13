package com.soturine.scanora.core.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soturine.scanora.core.data.files.ScanFileStore
import com.soturine.scanora.core.data.local.ScanoraDatabaseFactory
import kotlin.coroutines.cancellation.CancellationException

class CleanupExportsWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val database = ScanoraDatabaseFactory.create(applicationContext)
        return try {
            val pages = database.scanDao().getAllPages()
            val referencedSources = pages.map { it.sourceUri }.toSet()
            val fileStore = ScanFileStore(applicationContext)
            val cleanup = fileStore.cleanupOrphans(referencedSources)
            pages.forEach { page ->
                if (fileStore.managedFileExists(page.processedUri) == false) {
                    database.scanDao().clearProcessedUri(page.id)
                }
            }
            if (cleanup.failedFileCount == 0) Result.success() else Result.retry()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            Result.retry()
        } finally {
            database.close()
        }
    }
}

