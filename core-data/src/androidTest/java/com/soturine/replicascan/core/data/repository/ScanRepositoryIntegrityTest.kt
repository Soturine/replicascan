package com.soturine.replicascan.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.ScanMode
import com.soturine.replicascan.core.common.model.OcrTextResult
import com.soturine.replicascan.core.data.files.ManagedFilePolicy
import com.soturine.replicascan.core.data.files.ScanFileStore
import com.soturine.replicascan.core.data.local.ReplicaScanDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanRepositoryIntegrityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: ReplicaScanDatabase
    private lateinit var repository: DefaultScanRepository
    private lateinit var policy: ManagedFilePolicy

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ReplicaScanDatabase::class.java).build()
        policy = ManagedFilePolicy(context.filesDir, context.cacheDir)
        policy.sourceDirectory.mkdirs()
        repository = DefaultScanRepository(database.scanDao(), ScanFileStore(context))
    }

    @After
    fun tearDown() {
        database.close()
        policy.sourceDirectory.listFiles().orEmpty().forEach(File::delete)
    }

    @Test
    fun deletingPageRemovesPrivateFileAndReindexesRemainingPage() = runBlocking {
        val first = managedSource("delete-page-first.jpg")
        val second = managedSource("delete-page-second.jpg")
        val created = repository.createScan("Scan", ScanMode.DOCUMENT, listOf(first.path, second.path))

        val outcome = repository.deletePage(created.scanId, created.pageIds.first())

        assertThat(outcome.databaseDeleted).isTrue()
        assertThat(first.exists()).isFalse()
        assertThat(second.exists()).isTrue()
        assertThat(repository.getScan(created.scanId)?.pages?.single()?.index).isEqualTo(0)
    }

    @Test
    fun deletingScanDoesNotDeleteExternalFile() = runBlocking {
        val managed = managedSource("delete-scan-managed.jpg")
        val external = File(context.cacheDir, "external-user-file.jpg").apply { writeText("external") }
        val created = repository.createScan("Scan", ScanMode.DOCUMENT, listOf(managed.path, external.path))

        val outcome = repository.deleteScan(created.scanId)

        assertThat(repository.getScan(created.scanId)).isNull()
        assertThat(managed.exists()).isFalse()
        assertThat(external.exists()).isTrue()
        assertThat(outcome.skippedExternalCount).isEqualTo(1)
        external.delete()
        Unit
    }

    @Test
    fun metadataUpdatesAndSavePreserveAllPagesAndTheirState() = runBlocking {
        val sources = List(3) { index -> managedSource("metadata-$index.jpg") }
        val created = repository.createScan("Original", ScanMode.DOCUMENT, sources.map(File::getPath))
        val before = repository.getScan(created.scanId)!!

        repository.renameScan(created.scanId, "Renamed")
        repository.updateTags(created.scanId, listOf("tax", "2026"))
        repository.toggleFavorite(created.scanId)
        repository.markScanSaved(created.scanId)

        val after = repository.getScan(created.scanId)!!
        assertThat(after.pages).hasSize(3)
        assertThat(after.pages.map { it.id }).containsExactlyElementsIn(before.pages.map { it.id }).inOrder()
        assertThat(after.pages.map { it.index }).containsExactly(0, 1, 2).inOrder()
        assertThat(after.pages.map { it.sourceUri }).containsExactlyElementsIn(before.pages.map { it.sourceUri }).inOrder()
        assertThat(sources.all(File::exists)).isTrue()
        assertThat(after.isDraft).isFalse()
    }

    @Test
    fun updatePageOrderRejectsPartialDuplicateOrForeignIds() = runBlocking {
        val sources = List(3) { index -> managedSource("order-$index.jpg") }
        val created = repository.createScan("Scan", ScanMode.DOCUMENT, sources.map(File::getPath))

        assertFails { repository.updatePageOrder(created.scanId, created.pageIds.take(2)) }
        assertFails { repository.updatePageOrder(created.scanId, listOf(created.pageIds[0], created.pageIds[0], created.pageIds[2])) }
        assertFails { repository.updatePageOrder(created.scanId, listOf(created.pageIds[0], created.pageIds[1], "foreign")) }

        assertThat(repository.getScan(created.scanId)?.pages?.map { it.id })
            .containsExactlyElementsIn(created.pageIds)
            .inOrder()
    }

    @Test
    fun reordersWithUniqueIndicesWithoutReplacingPages() = runBlocking {
        val sources = List(3) { index -> managedSource("reorder-$index.jpg") }
        val created = repository.createScan("Scan", ScanMode.DOCUMENT, sources.map(File::getPath))
        val reversed = created.pageIds.reversed()

        repository.updatePageOrder(created.scanId, reversed)

        assertThat(repository.getScan(created.scanId)?.pages?.map { it.id })
            .containsExactlyElementsIn(reversed)
            .inOrder()
    }

    @Test
    fun fullTextSearchFindsTitleTagAndPersistedOcr() = runBlocking {
        val created = repository.createScan(
            title = "Travel receipt",
            mode = ScanMode.RECEIPT,
            sourceUris = listOf(managedSource("search.jpg").path),
            tags = listOf("tax|2026"),
        )
        repository.updatePageOcrArtifact(
            created.scanId,
            created.pageIds.single(),
            OcrTextResult.fromPlainText("Coffee total 42"),
        )

        assertThat(repository.observeScans("Coffee").first().single().id).isEqualTo(created.scanId)
        assertThat(repository.observeScans("Travel").first().single().id).isEqualTo(created.scanId)
        assertThat(repository.observeScans("tax").first().single().id).isEqualTo(created.scanId)
    }

    private suspend fun assertFails(block: suspend () -> Unit) {
        val result = runCatching { block() }
        assertThat(result.isFailure).isTrue()
    }

    private fun managedSource(name: String): File =
        File(policy.sourceDirectory, name).apply { writeText("private") }
}
