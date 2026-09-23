package com.soturine.replicascan.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import com.soturine.replicascan.core.common.image.CanonicalImageDecoder
import com.soturine.replicascan.core.common.image.DocumentQuadValidator
import com.soturine.replicascan.core.common.image.ImagePurpose
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.ImagePipelineSpec
import com.soturine.replicascan.core.common.model.PointValue
import com.soturine.replicascan.core.common.model.coerceNormalized
import com.soturine.replicascan.core.common.model.toPixelQuad
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies the user's approved geometry (manual crop, rotation) and optional look to the private
 * source. Document detection and cleanup happen earlier, inside ML Kit's scanner.
 */
class DefaultDocumentProcessingRepository(
    private val context: Context,
) : DocumentProcessingRepository {
    private val imageDecoder = CanonicalImageDecoder(context)

    override suspend fun renderPreview(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): String = withContext(Dispatchers.IO) {
        val bitmap = render(sourceUri, filterType, quad, rotationDegrees, maxDimension)
        saveBitmap(bitmap, prefix = "v${ImagePipelineSpec.VERSION}-${filterType.storageKey}-preview", quality = 88)
    }

    override suspend fun processPage(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
    ): String = withContext(Dispatchers.IO) {
        val bitmap = render(sourceUri, filterType, quad, rotationDegrees, ImagePurpose.PREVIEW.maxDimension)
        saveBitmap(bitmap, prefix = "v${ImagePipelineSpec.VERSION}-${filterType.storageKey}", quality = 92)
    }

    override suspend fun renderBitmap(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): Bitmap = withContext(Dispatchers.IO) {
        render(sourceUri, filterType, quad, rotationDegrees, maxDimension)
    }

    override suspend fun processForOcr(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
    ): String = withContext(Dispatchers.IO) {
        val bitmap = renderGeometry(sourceUri, quad, rotationDegrees, ImagePurpose.OCR.maxDimension)
        saveBitmap(bitmap, prefix = "v${ImagePipelineSpec.VERSION}-ocr", quality = 92)
    }

    private fun render(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): Bitmap = DocumentFilters.apply(renderGeometry(sourceUri, quad, rotationDegrees, maxDimension), filterType)

    private fun renderGeometry(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): Bitmap {
        val decoded = imageDecoder.decode(sourceUri, maxDimension)?.bitmap
            ?: throw PageRenderException(sourceUri)
        val cropQuad = quad?.coerceNormalized()
            ?.takeIf { it != DocumentQuad.FULL_PAGE && DocumentQuadValidator.isValidNormalized(it) }
        val cropped = cropQuad?.let { warpPerspective(decoded, it.toPixelQuad(decoded.width, decoded.height)) } ?: decoded
        val rotation = ImagePipelineSpec.normalizeRotation(rotationDegrees)
        return if (rotation == 0) cropped else rotateBitmap(cropped, rotation.toFloat())
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun warpPerspective(bitmap: Bitmap, quad: DocumentQuad): Bitmap {
        val targetWidth = max(distance(quad.topLeft, quad.topRight), distance(quad.bottomLeft, quad.bottomRight))
            .roundToInt().coerceAtLeast(1)
        val targetHeight = max(distance(quad.topLeft, quad.bottomLeft), distance(quad.topRight, quad.bottomRight))
            .roundToInt().coerceAtLeast(1)
        val source = quad.asList().flatMap { listOf(it.x, it.y) }.toFloatArray()
        val destination = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat(),
        )
        val matrix = Matrix()
        check(matrix.setPolyToPoly(source, 0, destination, 0, 4)) { "Invalid crop geometry" }
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }

    private fun distance(start: PointValue, end: PointValue): Float = hypot(end.x - start.x, end.y - start.y)

    private fun saveBitmap(bitmap: Bitmap, prefix: String, quality: Int): String {
        val dir = File(context.cacheDir, PROCESSED_DIRECTORY).apply { mkdirs() }
        val file = File(dir, "$prefix-${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) { "Could not encode page" }
        }
        return Uri.fromFile(file).toString()
    }

    private companion object {
        const val PROCESSED_DIRECTORY = "processed"
    }
}

/** The private page source could not be decoded; callers map this to a localized message. */
class PageRenderException(sourceUri: String) :
    IllegalStateException("Unreadable page source: ${sourceUri.substringAfterLast('/')}")
