package com.soturine.replicascan.core.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.ExportException
import com.soturine.replicascan.core.common.repository.ExportFailureReason
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultExportRepositoryTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val exported = mutableListOf<ExportedFile>()
    private val sources = mutableListOf<File>()

    @After
    fun cleanUp() {
        exported.forEach { file ->
            file.pathHint?.let { File(it).delete() } ?: context.contentResolver.delete(Uri.parse(file.uri), null, null)
        }
        sources.forEach(File::delete)
    }

    @Test
    fun exportedPdfContainsSearchableOcrLayer() = runBlocking {
        if (Build.VERSION.SDK_INT < 35) return@runBlocking
        val page = ScanPage("page", "scan", 0, source("searchable.png", Bitmap.CompressFormat.PNG).absolutePath, ocrText = "Invoice number 12345")
        val repository = DefaultExportRepository(context, RenderingProcessingRepository)

        val pdf = repository.exportPdf(scan(page), PdfQuality.BALANCED).also(exported::add)

        context.contentResolver.openFileDescriptor(Uri.parse(pdf.uri), "r")!!.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { pdfPage -> assertThat(pdfPage.searchText("Invoice")).isNotEmpty() }
            }
        }
        assertThat(pdf.searchableTextIncluded).isTrue()
    }

    @Test
    fun unreadablePageFailsExportWithPageIdentity() = runBlocking {
        val repository = DefaultExportRepository(context, RenderingProcessingRepository)
        val page = ScanPage(id = "page-id", scanId = "scan-id", index = 2, sourceUri = "/missing/source.jpg")

        val exception = runCatching { repository.exportPdf(scan(page), PdfQuality.BALANCED) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(ExportException::class.java)
        exception as ExportException
        assertThat(exception.reason).isEqualTo(ExportFailureReason.PAGE_UNREADABLE)
        assertThat(exception.pageId).isEqualTo("page-id")
        assertThat(exception.pageNumber).isEqualTo(3)
    }

    @Test
    fun pngExportNeverPassesThroughTheJpegDerivative() = runBlocking {
        val page = ScanPage("page", "scan", 0, source("filtered.jpg").absolutePath, filterType = DocumentFilterType.GRAYSCALE)
        val repository = DefaultExportRepository(context, RenderingProcessingRepository)

        val files = repository.exportImages(scan(page), ExportFormat.PNG).also(exported::addAll)

        assertThat(files).hasSize(1)
        assertThat(files.single().mimeType).isEqualTo("image/png")
        // RenderingProcessingRepository.processPage() throws: reaching here proves no JPEG intermediate was used.
    }

    @Test
    fun unmodifiedJpegPageIsCopiedByteForByte() = runBlocking {
        val sourceFile = source("original.jpg")
        val page = ScanPage("page", "scan", 0, sourceFile.absolutePath)
        val repository = DefaultExportRepository(context, RenderingProcessingRepository)

        val file = repository.exportImages(scan(page), ExportFormat.JPG).also(exported::addAll).single()

        val bytes = context.contentResolver.openInputStream(Uri.parse(file.uri))!!.use { it.readBytes() }
        assertThat(bytes).isEqualTo(sourceFile.readBytes())
    }

    @Test
    fun failedMultiPageImageExportRemovesPagesAlreadyWritten() = runBlocking {
        val good = ScanPage("good", "scan", 0, source("good.jpg").absolutePath, filterType = DocumentFilterType.ENHANCED)
        val broken = ScanPage("broken", "scan", 1, "/missing/second.jpg", filterType = DocumentFilterType.ENHANCED)
        val repository = DefaultExportRepository(context, RenderingProcessingRepository)
        val title = "Rollback ${System.nanoTime()}"

        val result = runCatching { repository.exportImages(scan(good, broken, title = title), ExportFormat.PNG) }

        assertThat(result.exceptionOrNull()).isInstanceOf(ExportException::class.java)
        assertThat(exportsNamedLike(ExportFileNameBuilder().buildPageName(title, 0, ExportFormat.PNG))).isEqualTo(0)
    }

    private fun exportsNamedLike(displayName: String): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(displayName),
                null,
            )?.use { it.count } ?: 0
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "replicascan-exports")
            dir.listFiles().orEmpty().count { it.name == displayName }
        }

    private fun source(name: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): File =
        File(context.cacheDir, "export-test-$name").apply {
            Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
                outputStream().use { compress(format, 95, it) }
                recycle()
            }
            sources += this
        }

    private fun scan(vararg pages: ScanPage, title: String = "Invoice") = ScanDocument(
        id = "scan",
        title = title,
        tags = emptyList(),
        isFavorite = false,
        createdAt = 1L,
        updatedAt = 1L,
        pages = pages.toList(),
        isDraft = true,
    )

    /** Renders from the source like production; the lossy file-based path must never be used by export. */
    private object RenderingProcessingRepository : DocumentProcessingRepository {
        override suspend fun renderPreview(
            sourceUri: String,
            filterType: DocumentFilterType,
            quad: DocumentQuad?,
            rotationDegrees: Int,
            maxDimension: Int,
        ): String = error("export must not render previews")

        override suspend fun processPage(
            sourceUri: String,
            filterType: DocumentFilterType,
            quad: DocumentQuad?,
            rotationDegrees: Int,
        ): String = error("export must not use the JPEG derivative")

        override suspend fun renderBitmap(
            sourceUri: String,
            filterType: DocumentFilterType,
            quad: DocumentQuad?,
            rotationDegrees: Int,
            maxDimension: Int,
        ): Bitmap = BitmapFactory.decodeFile(sourceUri) ?: error("unreadable $sourceUri")

        override suspend fun processForOcr(sourceUri: String, quad: DocumentQuad?, rotationDegrees: Int): String =
            error("unused")
    }
}
