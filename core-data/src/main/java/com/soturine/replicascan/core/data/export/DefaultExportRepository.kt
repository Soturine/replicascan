package com.soturine.replicascan.core.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.requiresDerivedImage
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.ExportRepository
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultExportRepository(
    private val context: Context,
    private val processingRepository: DocumentProcessingRepository,
    private val fileNameBuilder: ExportFileNameBuilder = ExportFileNameBuilder(),
) : ExportRepository {
    override suspend fun exportPdf(
        scan: ScanDocument,
        quality: PdfQuality,
        pageSize: PdfPageSize,
    ): ExportedFile = withContext(Dispatchers.IO) {
        val pages = scan.pages.sortedBy(ScanPage::index)
        require(pages.isNotEmpty()) { "O lote não possui páginas para exportar." }
        val document = PdfDocument()
        try {
            pages.forEachIndexed { pageNumber, page ->
                val bitmap = loadBitmap(page)
                var renderBitmap: Bitmap? = null
                try {
                    val (width, height) = resolvePageDimensions(pageSize, bitmap)
                    renderBitmap = scaleForPdf(bitmap, quality)
                    val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber + 1).create())
                    // The text is drawn first and then visually covered by the opaque scan. It stays in
                    // the PDF content stream for search/copy without changing the rendered document.
                    drawSearchableText(pdfPage.canvas, page.ocrText.orEmpty(), width, height)
                    pdfPage.canvas.drawBitmap(renderBitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), imagePaint(quality))
                    document.finishPage(pdfPage)
                } finally {
                    if (renderBitmap !== bitmap) renderBitmap?.recycle()
                    bitmap.recycle()
                }
            }
            writeStream(
                displayName = fileNameBuilder.buildBaseName(scan.title, ExportFormat.PDF),
                mimeType = ExportFormat.PDF.mimeType,
                searchableTextIncluded = pages.any { !it.ocrText.isNullOrBlank() },
            ) { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
    }

    override suspend fun exportImages(scan: ScanDocument, format: ExportFormat): List<ExportedFile> =
        withContext(Dispatchers.IO) {
            require(format == ExportFormat.JPG || format == ExportFormat.PNG)
            val pages = scan.pages.sortedBy(ScanPage::index)
            require(pages.isNotEmpty()) { "O lote não possui páginas para exportar." }
            val exported = mutableListOf<ExportedFile>()
            try {
                pages.forEach { page ->
                    val bitmap = loadBitmap(page)
                    try {
                        exported += writeStream(
                            displayName = fileNameBuilder.buildPageName(scan.title, page.index, format),
                            mimeType = format.mimeType,
                        ) { output ->
                            check(
                                bitmap.compress(
                                    if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                    92,
                                    output,
                                ),
                            ) { "Não foi possível codificar a página ${page.index + 1}." }
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
                exported
            } catch (throwable: Throwable) {
                exported.forEach(::deleteExport)
                throw throwable
            }
        }

    private fun resolvePageDimensions(pageSize: PdfPageSize, bitmap: Bitmap): Pair<Int, Int> = when (pageSize) {
        PdfPageSize.A4 -> if (bitmap.width >= bitmap.height) 842 to 595 else 595 to 842
        PdfPageSize.LETTER -> if (bitmap.width >= bitmap.height) 792 to 612 else 612 to 792
        PdfPageSize.AUTO -> {
            val width = if (bitmap.width >= bitmap.height) 842 else 595
            val height = (width * bitmap.height.toFloat() / bitmap.width).roundToInt().coerceIn(240, 1_440)
            width to height
        }
    }

    private fun imagePaint(quality: PdfQuality) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = quality != PdfQuality.COMPACT
    }

    private fun scaleForPdf(bitmap: Bitmap, quality: PdfQuality): Bitmap {
        val maximumLongSide = when (quality) {
            PdfQuality.COMPACT -> 1_600
            PdfQuality.BALANCED -> 2_400
            PdfQuality.HIGH -> 3_600
        }
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= maximumLongSide) return bitmap
        val scale = maximumLongSide.toFloat() / longSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun drawSearchableText(canvas: android.graphics.Canvas, text: String, width: Int, height: Int) {
        if (text.isBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
        }
        var y = 12f
        text.lineSequence().flatMap { it.chunked(100).asSequence() }.forEach { line ->
            canvas.drawText(line, 4f, y, paint)
            y += 10f
            if (y > height - 4f) y = 12f
        }
    }

    private suspend fun loadBitmap(page: ScanPage): Bitmap {
        try {
            val finalImageUri = if (page.requiresDerivedImage()) {
                processingRepository.processPage(page.sourceUri, page.filterType, page.quad, page.rotationDegrees)
            } else {
                page.canonicalUri
            }
            val uri = Uri.parse(finalImageUri)
            val stream = when {
                uri.scheme.isNullOrBlank() -> File(finalImageUri).inputStream()
                uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
                else -> context.contentResolver.openInputStream(uri)
            }
            return stream?.use(BitmapFactory::decodeStream) ?: throw ExportPageException(page.index, page.id)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ExportPageException) {
            throw exception
        } catch (exception: Exception) {
            throw ExportPageException(page.index, page.id, exception)
        }
    }

    private fun writeStream(
        displayName: String,
        mimeType: String,
        searchableTextIncluded: Boolean = false,
        writer: (OutputStream) -> Unit,
    ): ExportedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        writeToDownloads(displayName, mimeType, searchableTextIncluded, writer)
    } else {
        writeToAppStorage(displayName, mimeType, searchableTextIncluded, writer)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToDownloads(
        displayName: String,
        mimeType: String,
        searchableTextIncluded: Boolean,
        writer: (OutputStream) -> Unit,
    ): ExportedFile {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ReplicaScan"
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            },
        ) ?: error("Não foi possível preparar o arquivo para exportação.")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> writer(output); output.flush() }
                ?: error("Não foi possível gravar o arquivo exportado.")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.coerceAtLeast(0) ?: 0L
        return ExportedFile(displayName, uri.toString(), mimeType, size, "Downloads > ReplicaScan", "Downloads/ReplicaScan/$displayName", searchableTextIncluded)
    }

    private fun writeToAppStorage(
        displayName: String,
        mimeType: String,
        searchableTextIncluded: Boolean,
        writer: (OutputStream) -> Unit,
    ): ExportedFile {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val exportDir = File(baseDir, "replicascan-exports").apply { mkdirs() }
        val file = File(exportDir, displayName)
        try {
            file.outputStream().use { output -> writer(output); output.flush() }
        } catch (throwable: Throwable) {
            file.delete()
            throw throwable
        }
        return ExportedFile(
            displayName, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString(),
            mimeType, file.length(), "Armazenamento do app > replicascan-exports", file.absolutePath, searchableTextIncluded,
        )
    }

    private fun deleteExport(file: ExportedFile) {
        val path = file.pathHint
        if (path != null && File(path).isAbsolute) File(path).delete()
        else runCatching { context.contentResolver.delete(Uri.parse(file.uri), null, null) }
    }
}

class ExportPageException(
    pageIndex: Int,
    val pageId: String,
    cause: Throwable? = null,
) : IllegalStateException("Não foi possível exportar a página ${pageIndex + 1}.", cause)
