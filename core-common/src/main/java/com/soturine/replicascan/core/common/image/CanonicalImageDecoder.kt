package com.soturine.replicascan.core.common.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

enum class ImagePurpose(val maxDimension: Int, val maxPixels: Long) {
    THUMBNAIL(640, 400_000), PREVIEW(2_048, 3_200_000), DETECTION(1_600, 2_000_000),
    FILTER_PREVIEW(2_048, 3_200_000), OCR(2_600, 6_000_000),
    EXPORT_COMPACT(1_600, 2_500_000), EXPORT_BALANCED(2_600, 6_000_000), EXPORT_HIGH(4_096, 12_000_000),
}

data class CanonicalImage(
    val bitmap: Bitmap,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val exifOrientation: Int,
    val sampleSize: Int,
)

class CanonicalImageDecoder(private val context: Context) {
    fun decode(imageUri: String, purpose: ImagePurpose): CanonicalImage? =
        decode(imageUri, purpose.maxDimension, purpose.maxPixels)

    fun decode(imageUri: String, maxDimension: Int, maxPixels: Long = Long.MAX_VALUE): CanonicalImage? {
        val uri = Uri.parse(imageUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val orientation = readOrientation(uri)
        val orientedWidth = if (orientation.swapsAxes()) bounds.outHeight else bounds.outWidth
        val orientedHeight = if (orientation.swapsAxes()) bounds.outWidth else bounds.outHeight
        val dimensionRatio = max(orientedWidth, orientedHeight).toDouble() / maxDimension.coerceAtLeast(1)
        val pixelRatio = sqrt((orientedWidth.toDouble() * orientedHeight) / maxPixels.coerceAtLeast(1))
        val requiredRatio = max(1.0, max(dimensionRatio, pixelRatio))
        var sample = 1
        while (sample < ceil(requiredRatio).toInt()) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val decoded = open(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return CanonicalImage(decoded.applyExif(orientation), orientedWidth, orientedHeight, orientation, sample)
    }

    private fun readOrientation(uri: Uri): Int = runCatching {
        open(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun open(uri: Uri): InputStream? = when {
        uri.scheme.isNullOrBlank() -> File(uri.toString()).inputStream()
        uri.scheme == "file" -> File(uri.path.orEmpty()).inputStream()
        else -> context.contentResolver.openInputStream(uri)
    }
}

private fun Int.swapsAxes(): Boolean = this in setOf(
    ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_270,
)

private fun Bitmap.applyExif(orientation: Int): Bitmap {
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) return this
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { setRotate(180f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
