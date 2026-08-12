package com.soturine.scanora.core.data.files

import android.content.Context
import android.net.Uri
import com.soturine.scanora.core.common.model.DeletionOutcome
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportedSource(
    val inputIndex: Int,
    val stableUri: String,
)

enum class ImportFailureReason {
    UNSUPPORTED_TYPE,
    UNAVAILABLE_SOURCE,
    EMPTY_SOURCE,
    IO_ERROR,
}

data class ImportFailure(
    val inputIndex: Int,
    val reason: ImportFailureReason,
)

data class SourceImportResult(
    val imported: List<ImportedSource>,
    val failures: List<ImportFailure>,
) {
    val totalCount: Int
        get() = imported.size + failures.size
}

data class OrphanCleanupResult(
    val deletedFileCount: Int,
    val failedFileCount: Int,
)

interface SourceFileStore {
    suspend fun importSources(uriValues: List<String>): SourceImportResult

    suspend fun rollback(importedSources: List<ImportedSource>): DeletionOutcome
}

class ScanFileStore private constructor(
    filesDir: File,
    cacheDir: File,
    private val openSource: (String) -> InputStream?,
    private val resolveMimeType: (String) -> String?,
) : SourceFileStore {
    private val policy = ManagedFilePolicy(filesDir, cacheDir)
    private val cacheRoot = cacheDir

    constructor(context: Context) : this(
        filesDir = context.filesDir,
        cacheDir = context.cacheDir,
        openSource = { rawValue -> context.openSourceInputStream(rawValue) },
        resolveMimeType = { rawValue ->
            val uri = Uri.parse(rawValue)
            if (uri.scheme == "content") context.contentResolver.getType(uri) else null
        },
    )

    internal constructor(
        filesDir: File,
        cacheDir: File,
        openSource: (String) -> InputStream?,
        resolveMimeType: (String) -> String?,
        @Suppress("UNUSED_PARAMETER") testing: Unit = Unit,
    ) : this(filesDir, cacheDir, openSource, resolveMimeType)

    override suspend fun importSources(uriValues: List<String>): SourceImportResult = withContext(Dispatchers.IO) {
        policy.sourceDirectory.mkdirs()
        val imported = mutableListOf<ImportedSource>()
        val failures = mutableListOf<ImportFailure>()

        uriValues.forEachIndexed { index, rawValue ->
            val mimeType = runCatching { resolveMimeType(rawValue) }.getOrNull()
            val inferredExtension = rawValue.safePathComponent().substringAfterLast('.', "").lowercase(Locale.ROOT)
            val unsupportedMime = mimeType != null && mimeType.lowercase(Locale.ROOT) !in SUPPORTED_MIME_TYPES
            val unsupportedExtension = mimeType == null &&
                inferredExtension.isNotBlank() && inferredExtension !in SUPPORTED_EXTENSIONS
            if (unsupportedMime || unsupportedExtension) {
                failures += ImportFailure(index, ImportFailureReason.UNSUPPORTED_TYPE)
                return@forEachIndexed
            }

            val extension = safeExtension(rawValue, mimeType)
            val temporaryFile = File(policy.sourceDirectory, ".incoming-${UUID.randomUUID()}.tmp")
            val finalFile = File(policy.sourceDirectory, "source-${UUID.randomUUID()}.$extension")
            try {
                val copiedBytes = openSource(rawValue)?.use { input ->
                    temporaryFile.outputStream().use { output -> input.copyTo(output) }
                } ?: run {
                    failures += ImportFailure(index, ImportFailureReason.UNAVAILABLE_SOURCE)
                    return@forEachIndexed
                }
                if (copiedBytes <= 0L) {
                    temporaryFile.delete()
                    failures += ImportFailure(index, ImportFailureReason.EMPTY_SOURCE)
                    return@forEachIndexed
                }
                if (!temporaryFile.renameTo(finalFile)) {
                    temporaryFile.copyTo(finalFile, overwrite = false)
                    temporaryFile.delete()
                }
                imported += ImportedSource(index, finalFile.absolutePath)
                deleteOwnedCaptureInput(rawValue)
            } catch (exception: CancellationException) {
                temporaryFile.delete()
                finalFile.delete()
                rollbackInternal(imported.map(ImportedSource::stableUri))
                throw exception
            } catch (_: Exception) {
                temporaryFile.delete()
                finalFile.delete()
                failures += ImportFailure(index, ImportFailureReason.IO_ERROR)
            }
        }

        SourceImportResult(imported = imported, failures = failures)
    }

    override suspend fun rollback(importedSources: List<ImportedSource>): DeletionOutcome = withContext(Dispatchers.IO) {
        deleteManaged(importedSources.map(ImportedSource::stableUri))
    }

    suspend fun deletePageFiles(
        sourceUri: String,
        processedUri: String?,
    ): DeletionOutcome = withContext(Dispatchers.IO) {
        deleteManaged(listOfNotNull(sourceUri, processedUri))
    }

    suspend fun deleteScanFiles(pageUris: List<Pair<String, String?>>): DeletionOutcome = withContext(Dispatchers.IO) {
        deleteManaged(pageUris.flatMap { (source, processed) -> listOfNotNull(source, processed) })
    }

    suspend fun cleanupOrphans(
        referencedSourceUris: Set<String>,
        nowMillis: Long = System.currentTimeMillis(),
        gracePeriodMillis: Long = DEFAULT_ORPHAN_GRACE_MILLIS,
    ): OrphanCleanupResult = withContext(Dispatchers.IO) {
        val referencedSources = referencedSourceUris.mapNotNull { policy.resolve(it)?.file?.canonicalPath }.toSet()
        val threshold = nowMillis - gracePeriodMillis
        val candidates = buildList {
            addAll(
                policy.sourceDirectory.listFiles().orEmpty().filter { file ->
                    file.isFile && file.lastModified() < threshold && file.canonicalPath !in referencedSources
                },
            )
            addAll(policy.derivedDirectory.oldFiles(threshold))
            addAll(policy.sharedExportDirectory.oldFiles(threshold))
            addAll(cacheRoot.listFiles().orEmpty().filter { file ->
                file.isFile && file.name.startsWith("capture-") && file.lastModified() < threshold
            })
        }.distinctBy { it.canonicalPath }

        var deleted = 0
        var failed = 0
        candidates.forEach { file ->
            if (file.delete() || !file.exists()) deleted++ else failed++
        }
        OrphanCleanupResult(deletedFileCount = deleted, failedFileCount = failed)
    }

    fun managedFileExists(uriValue: String?): Boolean? =
        policy.resolve(uriValue)?.file?.exists()

    private fun deleteManaged(uriValues: List<String>): DeletionOutcome {
        var deleted = 0
        var missing = 0
        var skipped = 0
        var failed = 0
        uriValues.distinct().forEach { uriValue ->
            val managed = policy.resolve(uriValue)
            if (managed == null) {
                skipped++
            } else if (!managed.file.exists()) {
                missing++
            } else if (managed.file.delete()) {
                deleted++
            } else {
                failed++
            }
        }
        return DeletionOutcome(
            databaseDeleted = false,
            deletedFileCount = deleted,
            missingFileCount = missing,
            skippedExternalCount = skipped,
            failedFileCount = failed,
        )
    }

    private fun rollbackInternal(uriValues: List<String>) {
        deleteManaged(uriValues)
    }

    private fun deleteOwnedCaptureInput(rawValue: String) {
        val candidate = when {
            rawValue.startsWith("file:", ignoreCase = true) ->
                runCatching { File(URI(rawValue)) }.getOrNull()
            File(rawValue).isAbsolute -> File(rawValue)
            else -> null
        } ?: return
        val canonical = runCatching { candidate.canonicalFile }.getOrNull() ?: return
        val canonicalCache = runCatching { cacheRoot.canonicalFile }.getOrNull() ?: return
        if (canonical.parentFile == canonicalCache && canonical.name.startsWith("capture-")) {
            canonical.delete()
        }
    }

    private fun File.oldFiles(threshold: Long): List<File> =
        listFiles().orEmpty().filter { it.isFile && it.lastModified() < threshold }

    private fun safeExtension(
        rawValue: String,
        mimeType: String?,
    ): String = when (mimeType?.lowercase(Locale.ROOT)) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        else -> File(rawValue.safePathComponent()).extension
            .lowercase(Locale.ROOT)
            .takeIf { it in SUPPORTED_EXTENSIONS }
            ?: "jpg"
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif",
        )
        private const val DEFAULT_ORPHAN_GRACE_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

private fun String.safePathComponent(): String = when {
    startsWith("file:", ignoreCase = true) -> runCatching { URI(this).path }.getOrNull().orEmpty()
    else -> substringBefore('?').substringBefore('#')
}

private fun Context.openSourceInputStream(rawValue: String): InputStream? {
    val uri = Uri.parse(rawValue)
    return when {
        uri.scheme.isNullOrBlank() -> File(rawValue).inputStream()
        uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
        uri.scheme == "content" -> contentResolver.openInputStream(uri)
        else -> null
    }
}
