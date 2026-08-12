package com.soturine.scanora.core.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.data.files.ManagedFilePolicy
import com.soturine.scanora.core.data.files.ScanFileStore
import com.soturine.scanora.core.data.local.ScanoraDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanRepositoryIntegrityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: ScanoraDatabase
    private lateinit var repository: DefaultScanRepository
    private lateinit var policy: ManagedFilePolicy

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ScanoraDatabase::class.java).build()
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
    }

    private fun managedSource(name: String): File =
        File(policy.sourceDirectory, name).apply { writeText("private") }
}
