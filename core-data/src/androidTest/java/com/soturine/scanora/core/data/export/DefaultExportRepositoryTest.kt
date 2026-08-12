package com.soturine.scanora.core.data.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.ScanDocument
import com.soturine.scanora.core.common.model.ScanMode
import com.soturine.scanora.core.common.model.ScanPage
import com.soturine.scanora.core.common.repository.DocumentProcessingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultExportRepositoryTest {
    @Test
    fun unreadablePageFailsExportWithPageIdentity() = runBlocking {
        val repository = DefaultExportRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            processingRepository = PassthroughProcessingRepository,
        )
        val page = ScanPage(
            id = "page-id",
            scanId = "scan-id",
            index = 2,
            sourceUri = "/missing/source.jpg",
        )
        val scan = ScanDocument(
            id = "scan-id",
            title = "Teste",
            mode = ScanMode.DOCUMENT,
            tags = emptyList(),
            isFavorite = false,
            createdAt = 1L,
            updatedAt = 1L,
            pages = listOf(page),
            isDraft = true,
        )

        val exception = runCatching {
            repository.exportPdf(scan, PdfQuality.BALANCED)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(ExportPageException::class.java)
        assertThat((exception as ExportPageException).pageId).isEqualTo("page-id")
        assertThat(exception).hasMessageThat().contains("página 3")
    }

    private object PassthroughProcessingRepository : DocumentProcessingRepository {
        override suspend fun estimateDocumentQuad(imageUri: String): DocumentQuad = error("unused")

        override suspend fun renderPreview(
            sourceUri: String,
            filterType: DocumentFilterType,
            quad: DocumentQuad?,
            rotationDegrees: Int,
            maxDimension: Int,
        ): String = sourceUri

        override suspend fun processPage(
            sourceUri: String,
            filterType: DocumentFilterType,
            quad: DocumentQuad?,
            rotationDegrees: Int,
        ): String = sourceUri

        override suspend fun processForOcr(
            sourceUri: String,
            quad: DocumentQuad?,
            rotationDegrees: Int,
            preferReceiptMode: Boolean,
        ): String = sourceUri
    }
}
