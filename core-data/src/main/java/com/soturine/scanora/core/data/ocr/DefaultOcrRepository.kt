package com.soturine.scanora.core.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.soturine.scanora.core.common.image.CanonicalImageDecoder
import com.soturine.scanora.core.common.image.ImagePurpose
import com.soturine.scanora.core.common.model.OcrTextBlock
import com.soturine.scanora.core.common.model.OcrTextBounds
import com.soturine.scanora.core.common.model.OcrTextLine
import com.soturine.scanora.core.common.model.OcrTextResult
import com.soturine.scanora.core.common.repository.OcrRepository
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultOcrRepository(
    private val context: Context,
) : OcrRepository {
    private val imageDecoder = CanonicalImageDecoder(context)

    override suspend fun recognizeText(imageUri: String): OcrTextResult = withContext(Dispatchers.IO) {
        val bitmap = imageDecoder.decode(imageUri, ImagePurpose.OCR)?.bitmap ?: return@withContext OcrTextResult.Empty
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            formatRecognizedText(recognizer.process(inputImage).awaitResult())
        } finally {
            recognizer.close()
        }
    }

    private fun prepareForOcr(bitmap: Bitmap): Bitmap {
        val grayscale = toGrayscale(bitmap)
        val background = createBackgroundLuma(grayscale)
        val width = grayscale.width
        val height = grayscale.height
        val source = IntArray(width * height)
        grayscale.getPixels(source, 0, width, 0, 0, width, height)
        val balanced = IntArray(source.size)

        for (index in source.indices) {
            val baseValue = Color.red(source[index])
            val backgroundGray = background[index].coerceAtLeast(36)
            val scale = (1f + ((232 - backgroundGray) / 255f) * 0.34f).coerceIn(0.86f, 1.16f)
            val corrected = (baseValue * scale).roundToInt().coerceIn(0, 255)
            balanced[index] = Color.rgb(corrected, corrected, corrected)
        }

        val luma = IntArray(balanced.size) { index -> Color.red(balanced[index]) }
        val lower = percentile(luma, 0.06f)
        val upper = percentile(luma, 0.992f).coerceAtLeast(lower + 24)
        val output = IntArray(balanced.size)

        for (index in balanced.indices) {
            val value = Color.red(balanced[index])
            val stretched = (((value - lower) * 255f) / (upper - lower)).roundToInt().coerceIn(0, 255)
            output[index] = Color.rgb(stretched, stretched, stretched)
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun formatRecognizedText(result: Text): OcrTextResult {
        val blocks = result.textBlocks
            .mapNotNull { block ->
                val lines = block.lines
                    .mapNotNull { line ->
                        line.text.trim()
                            .takeIf(String::isNotBlank)
                            ?.let { text ->
                                OcrTextLine(
                                    text = text,
                                    bounds = line.boundingBox?.toOcrTextBounds(),
                                )
                            }
                    }
                lines.takeIf { it.isNotEmpty() }?.let { recognizedLines ->
                    OcrTextBlock(
                        lines = recognizedLines,
                        bounds = block.boundingBox?.toOcrTextBounds(),
                    )
                }
            }
        return OcrTextResult(
            blocks = blocks,
            fallbackText = result.text.trim(),
        )
    }

    private fun Rect.toOcrTextBounds(): OcrTextBounds =
        OcrTextBounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = IntArray(source.size)
        for (index in source.indices) {
            val color = source[index]
            val gray = (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color))
                .roundToInt()
                .coerceIn(0, 255)
            output[index] = Color.rgb(gray, gray, gray)
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun createBackgroundLuma(bitmap: Bitmap): IntArray {
        val small = Bitmap.createScaledBitmap(
            bitmap,
            max(bitmap.width / 14, 1),
            max(bitmap.height / 14, 1),
            true,
        )
        val blurred = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        val pixels = IntArray(blurred.width * blurred.height)
        blurred.getPixels(pixels, 0, blurred.width, 0, 0, blurred.width, blurred.height)
        return IntArray(pixels.size) { index -> Color.red(pixels[index]) }
    }

    private fun percentile(
        values: IntArray,
        fraction: Float,
    ): Int {
        val histogram = IntArray(256)
        values.forEach { histogram[it.coerceIn(0, 255)]++ }
        val target = (values.size * fraction.coerceIn(0f, 1f)).roundToInt()
        var seen = 0
        histogram.forEachIndexed { index, count ->
            seen += count
            if (seen >= target) return index
        }
        return 255
    }

}
