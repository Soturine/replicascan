package com.soturine.replicascan.core.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.soturine.replicascan.core.common.image.ImagePurpose
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.common.model.ExportedFile
import com.soturine.replicascan.core.common.model.PdfPageSize
import com.soturine.replicascan.core.common.model.PdfQuality
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.isUnmodified
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.common.repository.ExportException
import com.soturine.replicascan.core.common.repository.ExportFailureReason
import com.soturine.replicascan.core.common.repository.ExportRepository
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes exports sequentially, one page bitmap in memory at a time, straight into the final file.
 * Pages are rendered from the private source; no lossy intermediate file sits between source and output.
 */
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
        val pages = orderedPages(scan)
        val document = PdfDocument()
        try {
            pages.forEachIndexed { pageNumber, page ->
                val bitmap = renderPage(page, quality.maxLongSide())
                try {
                    val (width, height) = resolvePageDimensions(pageSize, bitmap)
                    val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber + 1).create())
                    // The text is drawn first and then visually covered by the opaque scan. It stays in
                    // the PDF content stream for search/copy without changing the rendered document.
                    drawSearchableText(pdfPage.canvas, page.ocrText.orEmpty(), height)
                    pdfPage.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), imagePaint(quality))
                    document.finishPage(pdfPage)
                } finally {
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
            val pages = orderedPages(scan)
            val exported = mutableListOf<ExportedFile>()
            try {
                pages.forEach { page ->
                    val displayName = fileNameBuilder.buildPageName(scan.title, page.index, format)
                    exported += if (format == ExportFormat.JPG && page.isUnmodified() && isJpeg(page.sourceUri)) {
                        // An untouched JPEG page is already the final artifact: copy bytes, never re-encode.
                        writeStream(displayName, format.mimeType) { output ->
                            openSource(page).use { input -> input.copyTo(output) }
                        }
                    } else {
                        val bitmap = renderPage(page, ImagePurpose.EXPORT_HIGH.maxDimension)
                        try {
                            writeStream(displayName, format.mimeType) { output ->
                                val encoded = if (format == ExportFormat.PNG) {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                                } else {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                                }
                                if (!encoded) throw ExportException(ExportFailureReason.WRITE_FAILED, page.index + 1, page.id)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                exported
            } catch (throwable: Throwable) {
                exported.forEach(::deleteExport)
                throw throwable
            }
        }

    private fun orderedPages(scan: ScanDocument): List<ScanPage> =
        scan.pages.sortedBy(ScanPage::index).ifEmpty { throw ExportException(ExportFailureReason.EMPTY_DOCUMENT) }

    private suspend fun renderPage(page: ScanPage, maxLongSide: Int): Bitmap {
        val rendered = try {
            processingRepository.renderBitmap(page.sourceUri, page.filterType, page.quad, page.rotationDegrees, maxLongSide)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw ExportException(ExportFailureReason.PAGE_UNREADABLE, page.index + 1, page.id, exception)
        }
        return scaleToLongSide(rendered, maxLongSide)
    }

    private fun openSource(page: ScanPage): InputStream = try {
        val uri = Uri.parse(page.sourceUri)
        when {
            uri.scheme.isNullOrBlank() -> File(page.sourceUri).inputStream()
            uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
            else -> context.contentResolver.openInputStream(uri)
        } ?: throw ExportException(ExportFailureReason.PAGE_UNREADABLE, page.index + 1, page.id)
    } catch (exception: ExportException) {
        throw exception
    } catch (exception: Exception) {
        throw ExportException(ExportFailureReason.PAGE_UNREADABLE, page.index + 1, page.id, exception)
    }

    private fun isJpeg(sourceUri: String): Boolean = runCatching {
        val uri = Uri.parse(sourceUri)
        val stream = when {
            uri.scheme.isNullOrBlank() -> File(sourceUri).inputStream()
            uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
            else -> context.contentResolver.openInputStream(uri)
        }
        stream?.use { input ->
            val header = ByteArray(3)
            input.read(header) == 3 &&
                header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()
        } ?: false
    }.getOrDefault(false)

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

    private fun PdfQuality.maxLongSide(): Int = when (this) {
        PdfQuality.COMPACT -> 1_600
        PdfQuality.BALANCED -> 2_400
        PdfQuality.HIGH -> 3_600
    }

    /** Decoding samples by powers of two, so the exact long-side budget is enforced here. */
    private fun scaleToLongSide(bitmap: Bitmap, maxLongSide: Int): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= maxLongSide) return bitmap
        val scale = maxLongSide.toFloat() / longSide
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt().coerceAtLeast(1),
            (bitmap.height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun drawSearchableText(canvas: Canvas, text: String, height: Int) {
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
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ReplicaScan")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            },
        ) ?: throw ExportException(ExportFailureReason.WRITE_FAILED)
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> writer(output); output.flush() }
                ?: throw ExportException(ExportFailureReason.WRITE_FAILED)
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.coerceAtLeast(0) ?: 0L
        return ExportedFile(displayName, uri.toString(), mimeType, size, pathHint = null, searchableTextIncluded = searchableTextIncluded)
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
            displayName = displayName,
            uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString(),
            mimeType = mimeType,
            sizeBytes = file.length(),
            pathHint = file.absolutePath,
            searchableTextIncluded = searchableTextIncluded,
        )
    }

    /** Only removes files written by the failing request itself. */
    private fun deleteExport(file: ExportedFile) {
        val path = file.pathHint
        if (path != null && File(path).isAbsolute) File(path).delete()
        else runCatching { context.contentResolver.delete(Uri.parse(file.uri), null, null) }
    }

    private companion object {
        const val JPEG_QUALITY = 92
    }
}
