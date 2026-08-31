package com.soturine.replicascan.core.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.data.local.entity.PageEntity
import com.soturine.replicascan.core.data.local.entity.ScanEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplicaScanDatabaseTest {
    private lateinit var database: ReplicaScanDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ReplicaScanDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertDeleteAndCascadePreservePageOrder() = runBlocking {
        val dao = database.scanDao()
        dao.insertScanWithPages(
            scan = scan("scan"),
            pages = listOf(page("page-1", "scan", 0), page("page-2", "scan", 1)),
        )

        dao.deletePageAndReindex("scan", "page-1", updatedAt = 2L)

        assertThat(dao.getPages("scan").map { it.id to it.pageIndex })
            .containsExactly("page-2" to 0)

        dao.deleteScan("scan")

        assertThat(dao.getPages("scan")).isEmpty()
    }

    private fun scan(id: String) = ScanEntity(
        id = id,
        title = "Scan",
        mode = "document",
        tags = "tag",
        isFavorite = true,
        createdAt = 1L,
        updatedAt = 1L,
        isDraft = false,
    )

    private fun page(id: String, scanId: String, index: Int) = PageEntity(
        id = id,
        scanId = scanId,
        pageIndex = index,
        sourceUri = "/private/$id.jpg",
        processedUri = null,
        filterType = "original_corrected",
        rotationDegrees = 90,
        quad = "0,0|1,0|1,1|0,1",
        ocrText = "texto",
    )
}
