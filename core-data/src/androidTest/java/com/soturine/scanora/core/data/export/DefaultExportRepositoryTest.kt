package com.soturine.scanora.core.data.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import java.io.File
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
    fun exportedPdfContainsSearchableOcrLayer() = runBlocking {
        if (Build.VERSION.SDK_INT < 35) return@runBlocking
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "searchable-source.png")
        Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val page = ScanPage("page", "scan", 0, source.absolutePath, ocrText = "Invoice number 12345")
        val scan = ScanDocument("scan", "Invoice", ScanMode.DOCUMENT, emptyList(), false, 1, 1, listOf(page), false)
        val repository = DefaultExportRepository(context, PassthroughProcessingRepository)

        val exported = repository.exportPdf(scan, PdfQuality.BALANCED)

        context.contentResolver.openFileDescriptor(Uri.parse(exported.uri), "r")!!.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { pdfPage ->
                    assertThat(pdfPage.searchText("Invoice")).isNotEmpty()
                }
            }
        }
        assertThat(exported.searchableTextIncluded).isTrue()
        context.contentResolver.delete(Uri.parse(exported.uri), null, null)
        source.delete()
    }

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
