package com.soturine.replicascan.core.data.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.soturine.replicascan.core.common.model.DocumentFilterType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Optional, non-destructive looks applied on top of the corrected page geometry.
 *
 * ML Kit's scanner already offers its own cleanup and filters, so ORIGINAL is a pure pass-through
 * and the remaining looks stay few and deterministic.
 */
internal object DocumentFilters {
    fun apply(bitmap: Bitmap, filterType: DocumentFilterType): Bitmap = when (filterType) {
        DocumentFilterType.ORIGINAL -> bitmap
        DocumentFilterType.ENHANCED -> colorEnhanced(bitmap)
        DocumentFilterType.GRAYSCALE -> documentGray(bitmap)
        DocumentFilterType.BLACK_WHITE -> documentBlackWhite(bitmap)
    }

    private fun documentBlackWhite(bitmap: Bitmap): Bitmap {
        val grayscale = toGrayscale(bitmap)
        val normalized = normalizeIllumination(
            bitmap = grayscale,
            targetBackground = 234,
            strength = 0.34f,
            minScale = 0.88f,
            maxScale = 1.15f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = 0.06f,
            upperPercentile = 0.992f,
        )
        val thresholded = softAdaptiveThreshold(
            grayscaleBitmap = contrasted,
            offset = 13,
            darkValue = 24,
            lightValue = 244,
            transition = 54,
        )
        return sharpen(
            compressHighlights(
                bitmap = blendBitmaps(
                    base = contrasted,
                    overlay = thresholded,
                    overlayAlpha = 0.72f,
                ),
                threshold = 226,
                amount = 0.2f,
            ),
            centerWeight = 4.02f,
            sideWeight = -0.74f,
        )
    }

    private fun documentGray(bitmap: Bitmap): Bitmap {
        val grayscale = toGrayscale(bitmap)
        val normalized = normalizeIllumination(
            bitmap = grayscale,
            targetBackground = 226,
            strength = 0.28f,
            minScale = 0.9f,
            maxScale = 1.12f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = 0.05f,
            upperPercentile = 0.988f,
        )
        val toned = blendBitmaps(
            base = contrasted,
            overlay = softAdaptiveThreshold(
                grayscaleBitmap = contrasted,
                offset = 24,
                darkValue = 44,
                lightValue = 230,
                transition = 86,
            ),
            overlayAlpha = 0.24f,
        )
        return compressHighlights(
            sharpen(
                bitmap = toned,
                centerWeight = 4.02f,
                sideWeight = -0.75f,
            ),
            threshold = 216,
            amount = 0.24f,
        )
    }

    private fun colorEnhanced(bitmap: Bitmap): Bitmap {
        val normalized = normalizeIllumination(
            bitmap = bitmap,
            targetBackground = 222,
            strength = 0.26f,
            minScale = 0.9f,
            maxScale = 1.14f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = 0.032f,
            upperPercentile = 0.985f,
        )
        val matrix = ColorMatrix(
            floatArrayOf(
                1.05f, 0f, 0f, 0f, 1f,
                0f, 1.05f, 0f, 0f, 1f,
                0f, 0f, 1.04f, 0f, 1f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        return sharpen(
            bitmap = blendBitmaps(
                base = compressHighlights(
                    bitmap = contrasted,
                    threshold = 232,
                    amount = 0.22f,
                ),
                overlay = applyColorMatrix(contrasted, matrix),
                overlayAlpha = 0.58f,
            ),
            centerWeight = 4.06f,
            sideWeight = -0.76f,
        )
    }

    private fun normalizeIllumination(
        bitmap: Bitmap,
        targetBackground: Int,
        strength: Float,
        minScale: Float,
        maxScale: Float,
    ): Bitmap {
        val background = createBackgroundLuma(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val normalized = IntArray(source.size)

        for (index in source.indices) {
            val backgroundGray = background[index].coerceAtLeast(36)
            val rawScale = 1f + ((targetBackground - backgroundGray) / 255f) * strength
            val scale = rawScale.coerceIn(minScale, maxScale)
            val sourceColor = source[index]
            normalized[index] = Color.rgb(
                (Color.red(sourceColor) * scale).roundToInt().coerceIn(0, 255),
                (Color.green(sourceColor) * scale).roundToInt().coerceIn(0, 255),
                (Color.blue(sourceColor) * scale).roundToInt().coerceIn(0, 255),
            )
        }
        return Bitmap.createBitmap(normalized, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun createBackgroundLuma(bitmap: Bitmap): IntArray {
        val grayscale = toGrayscale(bitmap)
        val downscaledWidth = max(grayscale.width / 14, 1)
        val downscaledHeight = max(grayscale.height / 14, 1)
        val small = Bitmap.createScaledBitmap(grayscale, downscaledWidth, downscaledHeight, true)
        val blurred = Bitmap.createScaledBitmap(small, grayscale.width, grayscale.height, true)
        val pixels = IntArray(blurred.width * blurred.height)
        blurred.getPixels(pixels, 0, blurred.width, 0, 0, blurred.width, blurred.height)
        return IntArray(pixels.size) { index -> Color.red(pixels[index]) }
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applyColorMatrix(
        bitmap: Bitmap,
        colorMatrix: ColorMatrix,
    ): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun sharpen(
        bitmap: Bitmap,
        centerWeight: Float,
        sideWeight: Float,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = source.copyOf()

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val center = source[index]
                val left = source[index - 1]
                val right = source[index + 1]
                val top = source[index - width]
                val bottom = source[index + width]

                output[index] = Color.rgb(
                    (
                        Color.red(center) * centerWeight +
                            (Color.red(left) + Color.red(right) + Color.red(top) + Color.red(bottom)) * sideWeight
                        ).roundToInt().coerceIn(0, 255),
                    (
                        Color.green(center) * centerWeight +
                            (Color.green(left) + Color.green(right) + Color.green(top) + Color.green(bottom)) * sideWeight
                        ).roundToInt().coerceIn(0, 255),
                    (
                        Color.blue(center) * centerWeight +
                            (Color.blue(left) + Color.blue(right) + Color.blue(top) + Color.blue(bottom)) * sideWeight
                        ).roundToInt().coerceIn(0, 255),
                )
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun stretchContrast(
        bitmap: Bitmap,
        lowerPercentile: Float,
        upperPercentile: Float,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val luma = IntArray(source.size) { index ->
            val color = source[index]
            (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)).roundToInt()
        }
        val lower = percentile(luma, lowerPercentile)
        val upper = percentile(luma, upperPercentile).coerceAtLeast(lower + 20)
        if (upper - lower < 20) return bitmap

        val adjusted = IntArray(source.size)
        for (index in source.indices) {
            val color = source[index]
            adjusted[index] = Color.rgb(
                stretchChannel(Color.red(color), lower, upper),
                stretchChannel(Color.green(color), lower, upper),
                stretchChannel(Color.blue(color), lower, upper),
            )
        }
        return Bitmap.createBitmap(adjusted, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun softAdaptiveThreshold(
        grayscaleBitmap: Bitmap,
        offset: Int,
        darkValue: Int,
        lightValue: Int,
        transition: Int,
    ): Bitmap {
        val width = grayscaleBitmap.width
        val height = grayscaleBitmap.height
        val source = IntArray(width * height)
        grayscaleBitmap.getPixels(source, 0, width, 0, 0, width, height)
        val background = createBackgroundLuma(grayscaleBitmap)
        val luma = IntArray(source.size) { index -> Color.red(source[index]) }
        val floor = max(otsuThreshold(luma) - 8, 78)
        val output = IntArray(source.size)

        for (index in source.indices) {
            val threshold = max(background[index] - offset, floor)
            val delta = threshold - luma[index]
            val value = when {
                delta <= -12 -> lightValue
                delta >= transition -> darkValue
                else -> {
                    val progress = ((delta + 12f) / (transition + 12f)).coerceIn(0f, 1f)
                    val eased = progress * progress * (3f - 2f * progress)
                    (lightValue - (lightValue - darkValue) * eased).roundToInt()
                }
            }
            output[index] = Color.rgb(value, value, value)
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun compressHighlights(
        bitmap: Bitmap,
        threshold: Int,
        amount: Float,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = IntArray(source.size)

        for (index in source.indices) {
            val color = source[index]
            output[index] = Color.rgb(
                compressHighlightChannel(Color.red(color), threshold, amount),
                compressHighlightChannel(Color.green(color), threshold, amount),
                compressHighlightChannel(Color.blue(color), threshold, amount),
            )
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun blendBitmaps(
        base: Bitmap,
        overlay: Bitmap,
        overlayAlpha: Float,
    ): Bitmap {
        val width = min(base.width, overlay.width)
        val height = min(base.height, overlay.height)
        if (width <= 0 || height <= 0) return base

        val safeAlpha = overlayAlpha.coerceIn(0f, 1f)
        val basePixels = IntArray(width * height)
        val overlayPixels = IntArray(width * height)
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)
        val output = IntArray(width * height)

        for (index in output.indices) {
            val baseColor = basePixels[index]
            val overlayColor = overlayPixels[index]
            output[index] = Color.rgb(
                ((Color.red(baseColor) * (1f - safeAlpha)) + (Color.red(overlayColor) * safeAlpha)).roundToInt().coerceIn(0, 255),
                ((Color.green(baseColor) * (1f - safeAlpha)) + (Color.green(overlayColor) * safeAlpha)).roundToInt().coerceIn(0, 255),
                ((Color.blue(baseColor) * (1f - safeAlpha)) + (Color.blue(overlayColor) * safeAlpha)).roundToInt().coerceIn(0, 255),
            )
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun compressHighlightChannel(
        value: Int,
        threshold: Int,
        amount: Float,
    ): Int {
        if (value <= threshold) return value
        val overflow = value - threshold
        return (threshold + overflow * (1f - amount)).roundToInt().coerceIn(0, 255)
    }

    private fun otsuThreshold(values: IntArray): Int {
        val histogram = IntArray(256)
        values.forEach { histogram[it.coerceIn(0, 255)]++ }
        val total = values.size
        var sum = 0.0
        for (index in histogram.indices) {
            sum += index * histogram[index]
        }

        var backgroundWeight = 0
        var backgroundSum = 0.0
        var maxVariance = 0.0
        var threshold = 127
        for (index in histogram.indices) {
            backgroundWeight += histogram[index]
            if (backgroundWeight == 0) continue
            val foregroundWeight = total - backgroundWeight
            if (foregroundWeight == 0) break
            backgroundSum += index * histogram[index]
            val backgroundMean = backgroundSum / backgroundWeight
            val foregroundMean = (sum - backgroundSum) / foregroundWeight
            val variance = backgroundWeight * foregroundWeight * (backgroundMean - foregroundMean) * (backgroundMean - foregroundMean)
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = index
            }
        }
        return threshold
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

    private fun stretchChannel(
        value: Int,
        lower: Int,
        upper: Int,
    ): Int = (((value - lower) * 255f) / (upper - lower)).roundToInt().coerceIn(0, 255)
}
