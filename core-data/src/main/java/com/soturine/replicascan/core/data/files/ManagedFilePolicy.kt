package com.soturine.replicascan.core.data.files

import java.io.File
import java.net.URI

enum class ManagedFileKind {
    SOURCE,
    DERIVED,
    SHARED_EXPORT,
}

data class ManagedFile(
    val file: File,
    val kind: ManagedFileKind,
)

class ManagedFilePolicy(
    filesDir: File,
    cacheDir: File,
) {
    val sourceDirectory = File(filesDir, SOURCE_DIRECTORY_NAME)
    val derivedDirectory = File(cacheDir, DERIVED_DIRECTORY_NAME)
    val sharedExportDirectory = File(cacheDir, SHARED_EXPORT_DIRECTORY_NAME)

    fun resolve(uriValue: String?): ManagedFile? {
        val candidate = uriValue?.takeIf(String::isNotBlank)?.toLocalFile() ?: return null
        val canonical = runCatching { candidate.canonicalFile }.getOrNull() ?: return null
        return when {
            canonical.isWithin(sourceDirectory) -> ManagedFile(canonical, ManagedFileKind.SOURCE)
            canonical.isWithin(derivedDirectory) -> ManagedFile(canonical, ManagedFileKind.DERIVED)
            canonical.isWithin(sharedExportDirectory) -> ManagedFile(canonical, ManagedFileKind.SHARED_EXPORT)
            else -> null
        }
    }

    private fun String.toLocalFile(): File? {
        val directFile = File(this)
        return when {
            directFile.isAbsolute -> directFile
            startsWith("file:", ignoreCase = true) -> runCatching { File(URI(this)) }.getOrNull()
            substringBefore(':', missingDelimiterValue = "").isNotBlank() -> null
            else -> directFile
        }
    }

    private fun File.isWithin(directory: File): Boolean {
        val root = runCatching { directory.canonicalPath.trimEnd(File.separatorChar) + File.separator }.getOrNull()
            ?: return false
        return canonicalPath.startsWith(root)
    }

    companion object {
        const val SOURCE_DIRECTORY_NAME = "scan-sources"
        const val DERIVED_DIRECTORY_NAME = "processed"
        const val SHARED_EXPORT_DIRECTORY_NAME = "shared-exports"
    }
}
