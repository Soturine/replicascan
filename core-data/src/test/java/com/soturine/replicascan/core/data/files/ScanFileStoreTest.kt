package com.soturine.replicascan.core.data.files

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class ScanFileStoreTest {
    private lateinit var root: File
    private lateinit var filesDir: File
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("replicascan-store").toFile()
        filesDir = File(root, "files").apply { mkdirs() }
        cacheDir = File(root, "cache").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun partialImportPreservesSuccessfulInputOrderAndReportsFailures() = runBlocking {
        val payloads = mapOf(
            "first.jpg" to "first".toByteArray(),
            "third.png" to "third".toByteArray(),
        )
        val store = testStore { value -> payloads[value]?.let(::ByteArrayInputStream) }

        val result = store.importSources(listOf("first.jpg", "missing.jpg", "third.png"))

        assertThat(result.imported.map { it.inputIndex }).containsExactly(0, 2).inOrder()
        assertThat(result.failures.map { it.inputIndex }).containsExactly(1)
        assertThat(result.imported.map { File(it.stableUri).readText() }).containsExactly("first", "third").inOrder()
    }

    @Test
    fun rollbackDeletesOnlyImportedManagedSources() = runBlocking {
        val external = File(root, "external.jpg").apply { writeText("keep") }
        val store = testStore { ByteArrayInputStream("private".toByteArray()) }
        val imported = store.importSources(listOf("input.jpg")).imported

        val outcome = store.rollback(imported + ImportedSource(1, external.absolutePath))

        assertThat(File(imported.single().stableUri).exists()).isFalse()
        assertThat(external.exists()).isTrue()
        assertThat(outcome.deletedFileCount).isEqualTo(1)
        assertThat(outcome.skippedExternalCount).isEqualTo(1)
    }

    @Test
    fun rejectsEmptyAndUnexpectedInputsWithoutLeavingFiles() = runBlocking {
        val store = ScanFileStore(
            filesDir = filesDir,
            cacheDir = cacheDir,
            openSource = { value ->
                if (value == "empty.jpg") ByteArrayInputStream(byteArrayOf()) else ByteArrayInputStream("pdf".toByteArray())
            },
            resolveMimeType = { value -> if (value == "document.pdf") "application/pdf" else "image/jpeg" },
        )

        val result = store.importSources(listOf("empty.jpg", "document.pdf"))

        assertThat(result.imported).isEmpty()
        assertThat(result.failures.map { it.reason })
            .containsExactly(ImportFailureReason.EMPTY_SOURCE, ImportFailureReason.UNSUPPORTED_TYPE)
            .inOrder()
        assertThat(ManagedFilePolicy(filesDir, cacheDir).sourceDirectory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun orphanCleanupKeepsReferencedAndRecentSources() = runBlocking {
        val policy = ManagedFilePolicy(filesDir, cacheDir)
        policy.sourceDirectory.mkdirs()
        val oldReferenced = File(policy.sourceDirectory, "referenced.jpg").apply { writeText("keep") }
        val oldOrphan = File(policy.sourceDirectory, "orphan.jpg").apply { writeText("delete") }
        val recentOrphan = File(policy.sourceDirectory, "recent.jpg").apply { writeText("keep") }
        val now = System.currentTimeMillis()
        oldReferenced.setLastModified(now - 10_000)
        oldOrphan.setLastModified(now - 10_000)
        recentOrphan.setLastModified(now)
        val store = testStore { null }

        store.cleanupOrphans(
            referencedSourceUris = setOf(oldReferenced.absolutePath),
            nowMillis = now,
            gracePeriodMillis = 5_000,
        )

        assertThat(oldReferenced.exists()).isTrue()
        assertThat(oldOrphan.exists()).isFalse()
        assertThat(recentOrphan.exists()).isTrue()
    }

    private fun testStore(openSource: (String) -> ByteArrayInputStream?): ScanFileStore =
        ScanFileStore(
            filesDir = filesDir,
            cacheDir = cacheDir,
            openSource = openSource,
            resolveMimeType = { null },
        )
}
