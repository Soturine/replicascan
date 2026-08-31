package com.soturine.replicascan.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.soturine.replicascan.core.common.image.CanonicalImageDecoder
import com.soturine.replicascan.core.common.image.AutomaticDocumentProfileRouter
import com.soturine.replicascan.core.common.image.DocumentQuadValidator
import com.soturine.replicascan.core.common.image.ImagePurpose
import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentProfile
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.ImagePipelineSpec
import com.soturine.replicascan.core.common.model.PointValue
import com.soturine.replicascan.core.common.model.coerceNormalized
import com.soturine.replicascan.core.common.repository.DocumentProcessingRepository
import com.soturine.replicascan.core.data.image.detection.DocumentDetector
import com.soturine.replicascan.core.data.image.detection.HeuristicDocumentDetector
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultDocumentProcessingRepository(
    private val context: Context,
    private val documentDetector: DocumentDetector = HeuristicDocumentDetector(),
) : DocumentProcessingRepository {
    private val imageDecoder = CanonicalImageDecoder(context)
    override suspend fun detectDocumentAutomatically(imageUri: String): DocumentDetectionResult = withContext(Dispatchers.Default) {
        val bitmap = imageDecoder.decode(imageUri, ImagePurpose.DETECTION)?.bitmap
            ?: return@withContext DocumentDetectionResult.noDocument()
        val general = documentDetector.detect(bitmap, DocumentProfile.GENERAL)
        val routedProfile = AutomaticDocumentProfileRouter.route(general)
        if (routedProfile == DocumentProfile.GENERAL) general
        else listOf(general, documentDetector.detect(bitmap, routedProfile)).maxBy { it.score }
    }

    override suspend fun detectDocument(
        imageUri: String,
        profile: DocumentProfile,
    ): DocumentDetectionResult = withContext(Dispatchers.Default) {
        val bitmap = imageDecoder.decode(imageUri, ImagePurpose.DETECTION)?.bitmap
            ?: return@withContext DocumentDetectionResult.noDocument()
        documentDetector.detect(bitmap, profile)
    }

    override suspend fun detectPreviewLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile,
    ): DocumentDetectionResult = withContext(Dispatchers.Default) {
        documentDetector.detectLuma(luma, width, height, profile)
    }

    override suspend fun renderPreview(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): String = renderPage(
        sourceUri = sourceUri,
        filterType = filterType,
        quad = quad,
        rotationDegrees = rotationDegrees,
        maxDimension = maxDimension,
        quality = 88,
        prefix = "v${ImagePipelineSpec.VERSION}-${filterType.storageKey}-preview",
    )

    override suspend fun processPage(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
    ): String = renderPage(
        sourceUri = sourceUri,
        filterType = filterType,
        quad = quad,
        rotationDegrees = rotationDegrees,
        maxDimension = 2800,
        quality = 93,
        prefix = "v${ImagePipelineSpec.VERSION}-${filterType.storageKey}",
    )

    override suspend fun processForOcr(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        preferReceiptMode: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val geometry = renderDocumentGeometry(
            sourceUri = sourceUri,
            quad = quad,
            rotationDegrees = rotationDegrees,
            maxDimension = 2600,
        )
        val prepared = prepareForOcr(geometry, preferReceiptMode)
        saveBitmap(
            bitmap = prepared,
            prefix = if (preferReceiptMode) {
                "v${ImagePipelineSpec.VERSION}-ocr-receipt"
            } else {
                "v${ImagePipelineSpec.VERSION}-ocr"
            },
            quality = 92,
        )
    }

    private suspend fun renderPage(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
        quality: Int,
        prefix: String,
    ): String = withContext(Dispatchers.IO) {
        var processed = renderDocumentGeometry(
            sourceUri = sourceUri,
            quad = quad,
            rotationDegrees = rotationDegrees,
            maxDimension = maxDimension,
        )
        processed = when (filterType) {
            DocumentFilterType.AUTO -> autoEnhance(processed)
            DocumentFilterType.ORIGINAL_CORRECTED -> enhanceOriginal(processed)
            DocumentFilterType.DOCUMENT_BLACK_WHITE -> documentBlackWhite(processed)
            DocumentFilterType.DOCUMENT_GRAY -> documentGray(processed)
            DocumentFilterType.COLOR_ENHANCED -> colorEnhanced(processed)
            DocumentFilterType.RECEIPT_HIGH_CONTRAST -> receiptHighContrast(processed)
        }

        saveBitmap(
            bitmap = processed,
            prefix = prefix,
            quality = quality,
        )
    }

    private fun renderDocumentGeometry(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): Bitmap {
        val bitmap = loadBitmap(sourceUri, maxDimension = maxDimension) ?: error("Não foi possível abrir a imagem.")
        val normalizedQuad = quad?.coerceNormalized()?.takeIf { DocumentQuadValidator.isValidNormalized(it) } ?: fullPageQuad()
        val effectiveQuad = scaleQuadToBitmap(
            quad = normalizedQuad,
            width = bitmap.width,
            height = bitmap.height,
        )
        var processed = warpPerspective(bitmap, effectiveQuad)
        processed = removeBlackBorders(processed)

        val normalizedRotation = ImagePipelineSpec.normalizeRotation(rotationDegrees)
        if (normalizedRotation != 0) {
            processed = rotateBitmap(processed, normalizedRotation.toFloat())
        }
        return processed
    }

    private fun autoEnhance(bitmap: Bitmap): Bitmap {
        val aspect = max(bitmap.width, bitmap.height).toFloat() / min(bitmap.width, bitmap.height).coerceAtLeast(1)
        if (aspect >= 2.25f) return receiptHighContrast(bitmap)
        var saturationTotal = 0f
        var samples = 0
        val step = max(min(bitmap.width, bitmap.height) / 72, 1)
        val hsv = FloatArray(3)
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                Color.colorToHSV(bitmap.getPixel(x, y), hsv)
                saturationTotal += hsv[1]
                samples++
            }
        }
        val averageSaturation = saturationTotal / samples.coerceAtLeast(1)
        return if (averageSaturation >= 0.12f) colorEnhanced(bitmap) else documentGray(bitmap)
    }

    private fun estimateQuadFromBounds(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        coarseBounds: Rect,
        strongThreshold: Float,
    ): DocumentQuad? {
        val searchRect = expandRect(
            rect = coarseBounds,
            width = width,
            height = height,
            horizontalFraction = 0.18f,
            verticalFraction = 0.18f,
        )
        val verticalSpan = max(coarseBounds.width() / 3, width / 8)
        val horizontalSpan = max(coarseBounds.height() / 3, height / 8)

        val leftSamples = collectVerticalBoundarySamples(
            luma = luma,
            edgeMagnitude = edgeMagnitude,
            width = width,
            height = height,
            anchorStart = coarseBounds.top,
            anchorEnd = coarseBounds.bottom,
            searchStart = searchRect.left,
            searchEnd = min(coarseBounds.left + verticalSpan, searchRect.right),
            expected = coarseBounds.left,
            insideToRight = true,
            threshold = strongThreshold,
        )
        val rightSamples = collectVerticalBoundarySamples(
            luma = luma,
            edgeMagnitude = edgeMagnitude,
            width = width,
            height = height,
            anchorStart = coarseBounds.top,
            anchorEnd = coarseBounds.bottom,
            searchStart = max(coarseBounds.right - verticalSpan, searchRect.left),
            searchEnd = searchRect.right,
            expected = coarseBounds.right,
            insideToRight = false,
            threshold = strongThreshold,
        )
        val topSamples = collectHorizontalBoundarySamples(
            luma = luma,
            edgeMagnitude = edgeMagnitude,
            width = width,
            height = height,
            anchorStart = coarseBounds.left,
            anchorEnd = coarseBounds.right,
            searchStart = searchRect.top,
            searchEnd = min(coarseBounds.top + horizontalSpan, searchRect.bottom),
            expected = coarseBounds.top,
            insideToBottom = true,
            threshold = strongThreshold,
        )
        val bottomSamples = collectHorizontalBoundarySamples(
            luma = luma,
            edgeMagnitude = edgeMagnitude,
            width = width,
            height = height,
            anchorStart = coarseBounds.left,
            anchorEnd = coarseBounds.right,
            searchStart = max(coarseBounds.bottom - horizontalSpan, searchRect.top),
            searchEnd = searchRect.bottom,
            expected = coarseBounds.bottom,
            insideToBottom = false,
            threshold = strongThreshold,
        )

        val leftLine = fitVerticalLine(leftSamples) ?: return null
        val rightLine = fitVerticalLine(rightSamples) ?: return null
        val topLine = fitHorizontalLine(topSamples) ?: return null
        val bottomLine = fitHorizontalLine(bottomSamples) ?: return null

        val candidateQuad = DocumentQuad(
            topLeft = clampPoint(intersect(leftLine, topLine), width, height),
            topRight = clampPoint(intersect(rightLine, topLine), width, height),
            bottomRight = clampPoint(intersect(rightLine, bottomLine), width, height),
            bottomLeft = clampPoint(intersect(leftLine, bottomLine), width, height),
        )

        if (!isReasonableQuad(candidateQuad, width, height)) return null

        val fallback = rectToQuadPixels(coarseBounds)
        val countConfidence = minOf(
            1f,
            (
                leftSamples.size +
                    rightSamples.size +
                    topSamples.size +
                    bottomSamples.size
                ) / 32f,
        )
        val scoreConfidence = minOf(
            1f,
            listOf(leftSamples, rightSamples, topSamples, bottomSamples)
                .flatMap { it }
                .map { it.score }
                .average()
                .toFloat() / (strongThreshold * 1.2f).coerceAtLeast(24f),
        )
        val candidateWeight = (0.48f + countConfidence * 0.28f + scoreConfidence * 0.24f).coerceIn(0.48f, 0.94f)
        val blended = blendQuad(
            fallback = fallback,
            candidate = candidateQuad,
            candidateWeight = candidateWeight,
        )
        val tightened = tightenQuad(
            quad = blended,
            amount = (0.017f - candidateWeight * 0.006f).coerceIn(0.008f, 0.015f),
        )
        return normalizeQuad(tightened, width, height)
    }

    private fun collectVerticalBoundarySamples(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        anchorStart: Int,
        anchorEnd: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insideToRight: Boolean,
        threshold: Float,
    ): List<BoundarySample> {
        if (anchorEnd <= anchorStart || searchEnd <= searchStart) return emptyList()
        val step = max((anchorEnd - anchorStart) / 12, 12)
        return buildList {
            var row = anchorStart + step / 2
            while (row < anchorEnd - step / 2) {
                findVerticalBoundaryAtRow(
                    luma = luma,
                    edgeMagnitude = edgeMagnitude,
                    width = width,
                    height = height,
                    row = row,
                    searchStart = searchStart,
                    searchEnd = searchEnd,
                    expected = expected,
                    insideToRight = insideToRight,
                    threshold = threshold,
                )?.let(::add)
                row += step
            }
        }
    }

    private fun collectHorizontalBoundarySamples(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        anchorStart: Int,
        anchorEnd: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insideToBottom: Boolean,
        threshold: Float,
    ): List<BoundarySample> {
        if (anchorEnd <= anchorStart || searchEnd <= searchStart) return emptyList()
        val step = max((anchorEnd - anchorStart) / 12, 12)
        return buildList {
            var column = anchorStart + step / 2
            while (column < anchorEnd - step / 2) {
                findHorizontalBoundaryAtColumn(
                    luma = luma,
                    edgeMagnitude = edgeMagnitude,
                    width = width,
                    height = height,
                    column = column,
                    searchStart = searchStart,
                    searchEnd = searchEnd,
                    expected = expected,
                    insideToBottom = insideToBottom,
                    threshold = threshold,
                )?.let(::add)
                column += step
            }
        }
    }

    private fun findVerticalBoundaryAtRow(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        row: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insideToRight: Boolean,
        threshold: Float,
    ): BoundarySample? {
        val y = row.coerceIn(2, height - 3)
        val minX = searchStart.coerceAtLeast(2)
        val maxX = searchEnd.coerceAtMost(width - 3)
        if (maxX <= minX) return null
        val searchRange = (maxX - minX).coerceAtLeast(1)
        var bestScore = Float.NEGATIVE_INFINITY
        var bestX = -1

        for (x in minX..maxX) {
            val gradient = localVerticalGradient(luma, width, height, x, y)
            val edge = edgeMagnitude[y * width + x]
            val inside = averageHorizontalLuma(
                luma = luma,
                width = width,
                height = height,
                x = x,
                y = y,
                startOffset = if (insideToRight) 2 else -10,
                endOffset = if (insideToRight) 10 else -2,
            )
            val outside = averageHorizontalLuma(
                luma = luma,
                width = width,
                height = height,
                x = x,
                y = y,
                startOffset = if (insideToRight) -10 else 2,
                endOffset = if (insideToRight) -2 else 10,
            )
            val brightnessSignal = inside - outside
            val distancePenalty = abs(x - expected) / searchRange.toFloat() * 13f
            val score = gradient * 0.7f + edge * 0.45f + brightnessSignal * 0.62f - distancePenalty
            if (score > bestScore) {
                bestScore = score
                bestX = x
            }
        }

        val acceptance = max(18f, threshold * 0.82f)
        return if (bestX != -1 && bestScore >= acceptance) {
            BoundarySample(anchor = y.toFloat(), value = bestX.toFloat(), score = bestScore)
        } else {
            null
        }
    }

    private fun findHorizontalBoundaryAtColumn(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        column: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insideToBottom: Boolean,
        threshold: Float,
    ): BoundarySample? {
        val x = column.coerceIn(2, width - 3)
        val minY = searchStart.coerceAtLeast(2)
        val maxY = searchEnd.coerceAtMost(height - 3)
        if (maxY <= minY) return null
        val searchRange = (maxY - minY).coerceAtLeast(1)
        var bestScore = Float.NEGATIVE_INFINITY
        var bestY = -1

        for (y in minY..maxY) {
            val gradient = localHorizontalGradient(luma, width, height, x, y)
            val edge = edgeMagnitude[y * width + x]
            val inside = averageVerticalLuma(
                luma = luma,
                width = width,
                height = height,
                x = x,
                y = y,
                startOffset = if (insideToBottom) 2 else -10,
                endOffset = if (insideToBottom) 10 else -2,
            )
            val outside = averageVerticalLuma(
                luma = luma,
                width = width,
                height = height,
                x = x,
                y = y,
                startOffset = if (insideToBottom) -10 else 2,
                endOffset = if (insideToBottom) -2 else 10,
            )
            val brightnessSignal = inside - outside
            val distancePenalty = abs(y - expected) / searchRange.toFloat() * 13f
            val score = gradient * 0.7f + edge * 0.45f + brightnessSignal * 0.62f - distancePenalty
            if (score > bestScore) {
                bestScore = score
                bestY = y
            }
        }

        val acceptance = max(18f, threshold * 0.82f)
        return if (bestY != -1 && bestScore >= acceptance) {
            BoundarySample(anchor = x.toFloat(), value = bestY.toFloat(), score = bestScore)
        } else {
            null
        }
    }

    private fun enhanceOriginal(bitmap: Bitmap): Bitmap {
        val normalized = normalizeIllumination(
            bitmap = bitmap,
            targetBackground = 224,
            strength = 0.28f,
            minScale = 0.9f,
            maxScale = 1.12f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = 0.03f,
            upperPercentile = 0.982f,
        )
        return sharpen(
            compressHighlights(
                bitmap = contrasted,
                threshold = 228,
                amount = 0.2f,
            ),
            centerWeight = 4.08f,
            sideWeight = -0.77f,
        )
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

    private fun receiptHighContrast(bitmap: Bitmap): Bitmap {
        val grayscale = toGrayscale(bitmap)
        val normalized = normalizeIllumination(
            bitmap = grayscale,
            targetBackground = 239,
            strength = 0.44f,
            minScale = 0.86f,
            maxScale = 1.22f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = 0.08f,
            upperPercentile = 0.996f,
        )
        return blendBitmaps(
            base = contrasted,
            overlay = softAdaptiveThreshold(
            grayscaleBitmap = contrasted,
                offset = 11,
                darkValue = 16,
                lightValue = 246,
                transition = 38,
            ),
            overlayAlpha = 0.82f,
        )
    }

    private fun prepareForOcr(
        bitmap: Bitmap,
        preferReceiptMode: Boolean,
    ): Bitmap {
        val grayscale = toGrayscale(bitmap)
        val normalized = normalizeIllumination(
            bitmap = grayscale,
            targetBackground = if (preferReceiptMode) 238 else 230,
            strength = if (preferReceiptMode) 0.42f else 0.3f,
            minScale = 0.86f,
            maxScale = if (preferReceiptMode) 1.2f else 1.14f,
        )
        val contrasted = stretchContrast(
            bitmap = normalized,
            lowerPercentile = if (preferReceiptMode) 0.075f else 0.05f,
            upperPercentile = if (preferReceiptMode) 0.996f else 0.99f,
        )
        return if (preferReceiptMode) {
            blendBitmaps(
                base = contrasted,
                overlay = softAdaptiveThreshold(
                    grayscaleBitmap = contrasted,
                    offset = 10,
                    darkValue = 14,
                    lightValue = 246,
                    transition = 34,
                ),
                overlayAlpha = 0.76f,
            )
        } else {
            sharpen(
                bitmap = compressHighlights(
                    bitmap = blendBitmaps(
                        base = contrasted,
                        overlay = softAdaptiveThreshold(
                            grayscaleBitmap = contrasted,
                            offset = 18,
                            darkValue = 34,
                            lightValue = 236,
                            transition = 72,
                        ),
                        overlayAlpha = 0.26f,
                    ),
                    threshold = 224,
                    amount = 0.18f,
                ),
                centerWeight = 4.02f,
                sideWeight = -0.75f,
            )
        }
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

    private fun rotateBitmap(
        bitmap: Bitmap,
        degrees: Float,
    ): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun warpPerspective(
        bitmap: Bitmap,
        quad: DocumentQuad,
    ): Bitmap {
        val targetWidth = max(
            distance(quad.topLeft, quad.topRight),
            distance(quad.bottomLeft, quad.bottomRight),
        ).roundToInt().coerceAtLeast(1)
        val targetHeight = max(
            distance(quad.topLeft, quad.bottomLeft),
            distance(quad.topRight, quad.bottomRight),
        ).roundToInt().coerceAtLeast(1)

        val source = floatArrayOf(
            quad.topLeft.x, quad.topLeft.y,
            quad.topRight.x, quad.topRight.y,
            quad.bottomRight.x, quad.bottomRight.y,
            quad.bottomLeft.x, quad.bottomLeft.y,
        )
        val destination = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat(),
        )

        val matrix = Matrix()
        check(matrix.setPolyToPoly(source, 0, destination, 0, 4)) { "Invalid crop geometry" }
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    private fun detectBoundsCandidates(
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        strongThreshold: Float,
        denseThreshold: Float,
    ): List<Rect> {
        val projected = detectProjectedBounds(edgeMagnitude, width, height, denseThreshold)
        val raw = detectStrongEdgeExtents(edgeMagnitude, width, height, strongThreshold)
        val bright = detectBrightDocumentBounds(luma, width, height)
        val merged = mergeCandidateBounds(
            projected = projected,
            raw = raw,
            bright = bright,
            width = width,
            height = height,
        )
        val consensus = intersectCandidateBounds(
            width = width,
            height = height,
            projected,
            bright,
            raw,
        )

        return buildList {
            merged?.let(::add)
            consensus?.let(::add)
            bright?.let(::add)
            projected?.let(::add)
            raw?.let(::add)
            add(fallbackRect(width, height))
        }
            .map { stabilizeBounds(it, width, height) }
            .filter { candidate ->
                candidate.width() >= (width * 0.24f).roundToInt() &&
                    candidate.height() >= (height * 0.24f).roundToInt()
            }
            .distinctBy { rect ->
                listOf(rect.left, rect.top, rect.right, rect.bottom)
            }
    }

    private fun detectProjectedBounds(
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        threshold: Float,
    ): Rect? {
        val rowScores = FloatArray(height)
        val columnScores = FloatArray(width)

        for (y in 0 until height) {
            var rowScore = 0f
            for (x in 0 until width) {
                val edge = edgeMagnitude[y * width + x]
                if (edge < threshold) continue
                rowScore += edge * centerBiasWeight(x, width)
                columnScores[x] += edge * centerBiasWeight(y, height)
            }
            rowScores[y] = rowScore
        }

        val top = findBoundary(rowScores, fromStart = true)
        val bottom = findBoundary(rowScores, fromStart = false)
        val left = findBoundary(columnScores, fromStart = true)
        val right = findBoundary(columnScores, fromStart = false)

        if (top == -1 || bottom == -1 || left == -1 || right == -1) return null
        if (left >= right || top >= bottom) return null
        return Rect(left, top, right, bottom)
    }

    private fun findBoundary(
        scores: FloatArray,
        fromStart: Boolean,
    ): Int {
        if (scores.isEmpty()) return -1
        val maxScore = scores.maxOrNull() ?: return -1
        val threshold = max(maxScore * 0.17f, 14f)
        val margin = (scores.size * 0.025f).roundToInt()
        val runLength = max(scores.size / 60, 3)
        val range = if (fromStart) {
            margin until scores.size - margin
        } else {
            (scores.size - margin - 1) downTo margin
        }
        var consecutive = 0
        for (index in range) {
            if (scores[index] >= threshold) {
                consecutive++
                if (consecutive >= runLength) {
                    return if (fromStart) index - runLength + 1 else index + runLength - 1
                }
            } else {
                consecutive = 0
            }
        }
        return -1
    }

    private fun detectStrongEdgeExtents(
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        threshold: Float,
    ): Rect? {
        val rowScores = FloatArray(height)
        val columnScores = FloatArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val edge = edgeMagnitude[y * width + x]
                if (edge < threshold) continue
                rowScores[y] += centerBiasWeight(x, width)
                columnScores[x] += centerBiasWeight(y, height)
            }
        }
        val top = findBoundary(rowScores, fromStart = true)
        val bottom = findBoundary(rowScores, fromStart = false)
        val left = findBoundary(columnScores, fromStart = true)
        val right = findBoundary(columnScores, fromStart = false)
        if (left >= right || top >= bottom) return null
        return Rect(left, top, right, bottom)
    }

    private fun detectBrightDocumentBounds(
        luma: IntArray,
        width: Int,
        height: Int,
    ): Rect? {
        val rowAverages = FloatArray(height)
        val columnAverages = FloatArray(width)
        val columnWeights = FloatArray(width)
        for (y in 0 until height) {
            var rowTotal = 0f
            var rowWeight = 0f
            for (x in 0 until width) {
                val value = luma[y * width + x].toFloat()
                val horizontalWeight = centerBiasWeight(x, width)
                val verticalWeight = centerBiasWeight(y, height)
                rowTotal += value * horizontalWeight
                rowWeight += horizontalWeight
                columnAverages[x] += value * verticalWeight
                columnWeights[x] += verticalWeight
            }
            rowAverages[y] = rowTotal / rowWeight.coerceAtLeast(1f)
        }
        for (index in columnAverages.indices) {
            columnAverages[index] /= columnWeights[index].coerceAtLeast(1f)
        }

        val smoothedRows = smoothSeries(rowAverages, radius = max(height / 80, 3))
        val smoothedColumns = smoothSeries(columnAverages, radius = max(width / 80, 3))
        val globalAverage = smoothedRows.average().toFloat()
        val centralAverage = smoothedRows
            .slice(height / 5 until (height - height / 5).coerceAtLeast(height / 5 + 1))
            .average()
            .toFloat()
        val threshold = min(max(globalAverage + 5f, centralAverage - 18f), 236f)

        val top = findSeriesBoundary(smoothedRows, threshold, fromStart = true)
        val bottom = findSeriesBoundary(smoothedRows, threshold, fromStart = false)
        val left = findSeriesBoundary(smoothedColumns, threshold, fromStart = true)
        val right = findSeriesBoundary(smoothedColumns, threshold, fromStart = false)

        if (top == -1 || bottom == -1 || left == -1 || right == -1) return null
        if (left >= right || top >= bottom) return null
        return Rect(left, top, right, bottom)
    }

    private fun smoothSeries(
        values: FloatArray,
        radius: Int,
    ): FloatArray {
        if (values.isEmpty()) return values
        val output = FloatArray(values.size)
        for (index in values.indices) {
            var total = 0f
            var count = 0
            val start = max(0, index - radius)
            val end = min(values.lastIndex, index + radius)
            for (cursor in start..end) {
                total += values[cursor]
                count++
            }
            output[index] = total / count.coerceAtLeast(1)
        }
        return output
    }

    private fun findSeriesBoundary(
        values: FloatArray,
        threshold: Float,
        fromStart: Boolean,
    ): Int {
        if (values.isEmpty()) return -1
        val margin = max((values.size * 0.03f).roundToInt(), 2)
        val runLength = max(values.size / 50, 3)
        val range = if (fromStart) {
            margin until (values.size - margin)
        } else {
            (values.size - margin - 1) downTo margin
        }
        var consecutive = 0
        var boundary = -1
        for (index in range) {
            if (values[index] >= threshold) {
                consecutive++
                boundary = index
                if (consecutive >= runLength) {
                    return if (fromStart) index - runLength + 1 else index + runLength - 1
                }
            } else {
                consecutive = 0
                boundary = -1
            }
        }
        return boundary
    }

    private fun centerBiasWeight(
        index: Int,
        size: Int,
    ): Float {
        if (size <= 1) return 1f
        val center = (size - 1) / 2f
        val distance = abs(index - center) / center.coerceAtLeast(1f)
        return 0.72f + (1f - distance).coerceIn(0f, 1f) * 0.28f
    }

    private fun intersectCandidateBounds(
        width: Int,
        height: Int,
        vararg candidates: Rect?,
    ): Rect? {
        val valid = candidates.filterNotNull()
        if (valid.size < 2) return null
        val intersection = Rect(
            valid.maxOf { it.left },
            valid.maxOf { it.top },
            valid.minOf { it.right },
            valid.minOf { it.bottom },
        )
        if (intersection.width() < (width * 0.22f).roundToInt()) return null
        if (intersection.height() < (height * 0.22f).roundToInt()) return null
        return intersection
    }

    private fun mergeCandidateBounds(
        projected: Rect?,
        raw: Rect?,
        bright: Rect?,
        width: Int,
        height: Int,
    ): Rect? {
        val candidates = buildList {
            projected?.let { add(it to 1.2f) }
            raw?.let { add(it to 0.95f) }
            bright?.let { add(it to 1.35f) }
        }
        if (candidates.isEmpty()) return null

        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f
        var totalWeight = 0f
        candidates.forEach { (rect, weight) ->
            left += rect.left * weight
            top += rect.top * weight
            right += rect.right * weight
            bottom += rect.bottom * weight
            totalWeight += weight
        }
        val merged = Rect(
            (left / totalWeight).roundToInt(),
            (top / totalWeight).roundToInt(),
            (right / totalWeight).roundToInt(),
            (bottom / totalWeight).roundToInt(),
        )
        val minWidth = (width * 0.3f).roundToInt()
        val minHeight = (height * 0.3f).roundToInt()
        if (merged.width() < minWidth || merged.height() < minHeight) return null
        return merged
    }

    private fun stabilizeBounds(
        bounds: Rect,
        width: Int,
        height: Int,
    ): Rect {
        val expandX = (bounds.width() * 0.018f).roundToInt()
        val expandY = (bounds.height() * 0.018f).roundToInt()
        return Rect(
            max(bounds.left - expandX, 0),
            max(bounds.top - expandY, 0),
            min(bounds.right + expandX, width - 1),
            min(bounds.bottom + expandY, height - 1),
        )
    }

    private fun scoreQuadCandidate(
        quad: DocumentQuad,
        luma: IntArray,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
        referenceThreshold: Float,
    ): Float {
        val pixelQuad = scaleQuadToBitmap(quad, width, height)
        val bounds = quadBounds(pixelQuad)
        val areaRatio = (bounds.width() * bounds.height()).toFloat() / (width * height).toFloat()
        val areaScore = closenessScore(
            value = areaRatio,
            ideal = 0.58f,
            tolerance = 0.34f,
        )
        val aspectScore = closenessScore(
            value = bounds.width().toFloat() / bounds.height().coerceAtLeast(1).toFloat(),
            ideal = 0.78f,
            tolerance = 0.58f,
        )
        val centerScore = centerAlignmentScore(bounds, width, height)
        val edgeScore = (sampleQuadEdgeStrength(pixelQuad, edgeMagnitude, width, height) / (referenceThreshold * 1.18f).coerceAtLeast(24f))
            .coerceIn(0f, 1.2f)
        val brightnessScore = sampleQuadBrightnessContrast(pixelQuad, luma, width, height)
        val geometryScore = quadGeometryScore(pixelQuad, width, height)

        return (
            edgeScore * 0.34f +
                brightnessScore * 0.24f +
                areaScore * 0.18f +
                centerScore * 0.12f +
                aspectScore * 0.07f +
                geometryScore * 0.05f
            )
    }

    private fun quadBounds(quad: DocumentQuad): Rect {
        val xs = listOf(quad.topLeft.x, quad.topRight.x, quad.bottomRight.x, quad.bottomLeft.x)
        val ys = listOf(quad.topLeft.y, quad.topRight.y, quad.bottomRight.y, quad.bottomLeft.y)
        return Rect(
            xs.minOrNull()?.roundToInt() ?: 0,
            ys.minOrNull()?.roundToInt() ?: 0,
            xs.maxOrNull()?.roundToInt() ?: 0,
            ys.maxOrNull()?.roundToInt() ?: 0,
        )
    }

    private fun closenessScore(
        value: Float,
        ideal: Float,
        tolerance: Float,
    ): Float = (1f - (abs(value - ideal) / tolerance.coerceAtLeast(0.0001f))).coerceIn(0f, 1f)

    private fun centerAlignmentScore(
        bounds: Rect,
        width: Int,
        height: Int,
    ): Float {
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val deltaX = abs(centerX - width / 2f) / (width / 2f).coerceAtLeast(1f)
        val deltaY = abs(centerY - height / 2f) / (height / 2f).coerceAtLeast(1f)
        return (1f - (deltaX * 0.55f + deltaY * 0.45f)).coerceIn(0f, 1f)
    }

    private fun sampleQuadEdgeStrength(
        quad: DocumentQuad,
        edgeMagnitude: FloatArray,
        width: Int,
        height: Int,
    ): Float {
        val sides = listOf(
            quad.topLeft to quad.topRight,
            quad.topRight to quad.bottomRight,
            quad.bottomRight to quad.bottomLeft,
            quad.bottomLeft to quad.topLeft,
        )
        var total = 0f
        sides.forEach { side ->
            total += sampleLineAverage(
                values = edgeMagnitude,
                width = width,
                height = height,
                start = side.first,
                end = side.second,
                samples = 28,
            )
        }
        return total / sides.size
    }

    private fun sampleQuadBrightnessContrast(
        quad: DocumentQuad,
        luma: IntArray,
        width: Int,
        height: Int,
    ): Float {
        val bounds = quadBounds(quad)
        val insetX = max(bounds.width() / 14, 6)
        val insetY = max(bounds.height() / 14, 6)
        val ringSizeX = max(bounds.width() / 18, 5)
        val ringSizeY = max(bounds.height() / 18, 5)

        val inside = averageLumaRect(
            luma = luma,
            width = width,
            height = height,
            left = bounds.left + insetX,
            top = bounds.top + insetY,
            right = bounds.right - insetX,
            bottom = bounds.bottom - insetY,
        )
        val outside = averageLumaRing(
            luma = luma,
            width = width,
            height = height,
            bounds = bounds,
            ringSizeX = ringSizeX,
            ringSizeY = ringSizeY,
        )

        return ((inside - outside + 18f) / 78f).coerceIn(0f, 1f)
    }

    private fun averageLumaRing(
        luma: IntArray,
        width: Int,
        height: Int,
        bounds: Rect,
        ringSizeX: Int,
        ringSizeY: Int,
    ): Float {
        val samples = buildList<Float> {
            averageLumaRect(
                luma = luma,
                width = width,
                height = height,
                left = bounds.left - ringSizeX,
                top = bounds.top,
                right = bounds.left,
                bottom = bounds.bottom,
            ).takeIf { !it.isNaN() }?.let(::add)
            averageLumaRect(
                luma = luma,
                width = width,
                height = height,
                left = bounds.right,
                top = bounds.top,
                right = bounds.right + ringSizeX,
                bottom = bounds.bottom,
            ).takeIf { !it.isNaN() }?.let(::add)
            averageLumaRect(
                luma = luma,
                width = width,
                height = height,
                left = bounds.left,
                top = bounds.top - ringSizeY,
                right = bounds.right,
                bottom = bounds.top,
            ).takeIf { !it.isNaN() }?.let(::add)
            averageLumaRect(
                luma = luma,
                width = width,
                height = height,
                left = bounds.left,
                top = bounds.bottom,
                right = bounds.right,
                bottom = bounds.bottom + ringSizeY,
            ).takeIf { !it.isNaN() }?.let(::add)
        }
        return if (samples.isEmpty()) 0f else samples.average().toFloat()
    }

    private fun averageLumaRect(
        luma: IntArray,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): Float {
        val safeLeft = left.coerceIn(0, width - 1)
        val safeTop = top.coerceIn(0, height - 1)
        val safeRight = right.coerceIn(safeLeft + 1, width)
        val safeBottom = bottom.coerceIn(safeTop + 1, height)
        if (safeRight <= safeLeft || safeBottom <= safeTop) return Float.NaN
        var total = 0L
        var count = 0
        for (y in safeTop until safeBottom) {
            for (x in safeLeft until safeRight) {
                total += luma[y * width + x]
                count++
            }
        }
        return if (count == 0) Float.NaN else total.toFloat() / count
    }

    private fun quadGeometryScore(
        quad: DocumentQuad,
        width: Int,
        height: Int,
    ): Float {
        val normalized = normalizeQuad(quad, width, height)
        val topWidth = distance(normalized.topLeft, normalized.topRight)
        val bottomWidth = distance(normalized.bottomLeft, normalized.bottomRight)
        val leftHeight = distance(normalized.topLeft, normalized.bottomLeft)
        val rightHeight = distance(normalized.topRight, normalized.bottomRight)
        val widthScore = closenessScore(
            value = topWidth / bottomWidth.coerceAtLeast(0.001f),
            ideal = 1f,
            tolerance = 0.45f,
        )
        val heightScore = closenessScore(
            value = leftHeight / rightHeight.coerceAtLeast(0.001f),
            ideal = 1f,
            tolerance = 0.45f,
        )
        return (widthScore + heightScore) / 2f
    }

    private fun sampleLineAverage(
        values: FloatArray,
        width: Int,
        height: Int,
        start: PointValue,
        end: PointValue,
        samples: Int,
    ): Float {
        if (samples <= 1) return 0f
        var total = 0f
        repeat(samples) { index ->
            val progress = index / (samples - 1f)
            val x = (start.x + (end.x - start.x) * progress).roundToInt().coerceIn(0, width - 1)
            val y = (start.y + (end.y - start.y) * progress).roundToInt().coerceIn(0, height - 1)
            total += values[y * width + x]
        }
        return total / samples
    }

    private fun smoothLuma(
        luma: IntArray,
        width: Int,
        height: Int,
    ): IntArray {
        val output = luma.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var total = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        total += luma[(y + ky) * width + (x + kx)]
                    }
                }
                output[y * width + x] = total / 9
            }
        }
        return output
    }

    private fun sobelEdges(
        luma: IntArray,
        width: Int,
        height: Int,
    ): FloatArray {
        val edges = FloatArray(width * height)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx =
                    -luma[(y - 1) * width + (x - 1)] - 2 * luma[y * width + (x - 1)] - luma[(y + 1) * width + (x - 1)] +
                        luma[(y - 1) * width + (x + 1)] + 2 * luma[y * width + (x + 1)] + luma[(y + 1) * width + (x + 1)]
                val gy =
                    -luma[(y - 1) * width + (x - 1)] - 2 * luma[(y - 1) * width + x] - luma[(y - 1) * width + (x + 1)] +
                        luma[(y + 1) * width + (x - 1)] + 2 * luma[(y + 1) * width + x] + luma[(y + 1) * width + (x + 1)]
                edges[y * width + x] = hypot(gx.toFloat(), gy.toFloat())
            }
        }
        return edges
    }

    private fun localVerticalGradient(
        luma: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ): Float {
        var total = 0f
        for (offset in -1..1) {
            val clampedY = (y + offset).coerceIn(1, height - 2)
            total += abs(
                luma[clampedY * width + (x + 1)] - luma[clampedY * width + (x - 1)],
            )
        }
        return total / 3f
    }

    private fun localHorizontalGradient(
        luma: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ): Float {
        var total = 0f
        for (offset in -1..1) {
            val clampedX = (x + offset).coerceIn(1, width - 2)
            total += abs(
                luma[(y + 1) * width + clampedX] - luma[(y - 1) * width + clampedX],
            )
        }
        return total / 3f
    }

    private fun averageHorizontalLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        startOffset: Int,
        endOffset: Int,
    ): Float {
        var total = 0f
        var count = 0
        val minOffset = min(startOffset, endOffset)
        val maxOffset = max(startOffset, endOffset)
        for (offsetX in minOffset..maxOffset) {
            val sampleX = (x + offsetX).coerceIn(0, width - 1)
            for (offsetY in -1..1) {
                val sampleY = (y + offsetY).coerceIn(0, height - 1)
                total += luma[sampleY * width + sampleX]
                count++
            }
        }
        return total / count.coerceAtLeast(1)
    }

    private fun averageVerticalLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        startOffset: Int,
        endOffset: Int,
    ): Float {
        var total = 0f
        var count = 0
        val minOffset = min(startOffset, endOffset)
        val maxOffset = max(startOffset, endOffset)
        for (offsetY in minOffset..maxOffset) {
            val sampleY = (y + offsetY).coerceIn(0, height - 1)
            for (offsetX in -1..1) {
                val sampleX = (x + offsetX).coerceIn(0, width - 1)
                total += luma[sampleY * width + sampleX]
                count++
            }
        }
        return total / count.coerceAtLeast(1)
    }

    private fun toLumaArray(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return IntArray(pixels.size) { index ->
            val color = pixels[index]
            (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)).roundToInt()
        }
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

    private fun removeBlackBorders(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        fun rowAverage(y: Int): Int {
            var total = 0
            for (x in 0 until bitmap.width) {
                total += Color.red(pixels[y * bitmap.width + x])
            }
            return total / bitmap.width
        }

        fun columnAverage(x: Int): Int {
            var total = 0
            for (y in 0 until bitmap.height) {
                total += Color.red(pixels[y * bitmap.width + x])
            }
            return total / bitmap.height
        }

        var top = 0
        var bottom = bitmap.height - 1
        var left = 0
        var right = bitmap.width - 1
        while (top < bottom && rowAverage(top) < 24) top++
        while (bottom > top && rowAverage(bottom) < 24) bottom--
        while (left < right && columnAverage(left) < 24) left++
        while (right > left && columnAverage(right) < 24) right--

        val cropWidth = (right - left + 1).coerceAtLeast(1)
        val cropHeight = (bottom - top + 1).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun saveBitmap(
        bitmap: Bitmap,
        prefix: String,
        quality: Int,
    ): String {
        val dir = File(context.cacheDir, "processed").apply { mkdirs() }
        val file = File(dir, "${prefix}-${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        return Uri.fromFile(file).toString()
    }

    private fun loadBitmap(
        imageUri: String,
        maxDimension: Int,
    ): Bitmap? = imageDecoder.decode(imageUri, maxDimension)?.bitmap

    private fun fallbackQuad(): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue(0.07f, 0.07f),
            topRight = PointValue(0.93f, 0.07f),
            bottomRight = PointValue(0.93f, 0.93f),
            bottomLeft = PointValue(0.07f, 0.93f),
        )

    private fun fullPageQuad(): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue(0f, 0f),
            topRight = PointValue(1f, 0f),
            bottomRight = PointValue(1f, 1f),
            bottomLeft = PointValue(0f, 1f),
        )

    private fun fallbackRect(
        width: Int,
        height: Int,
    ): Rect =
        Rect(
            (width * 0.07f).roundToInt(),
            (height * 0.07f).roundToInt(),
            (width * 0.93f).roundToInt(),
            (height * 0.93f).roundToInt(),
        )

    private fun scaleQuadToBitmap(
        quad: DocumentQuad,
        width: Int,
        height: Int,
    ): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue(quad.topLeft.x * width, quad.topLeft.y * height),
            topRight = PointValue(quad.topRight.x * width, quad.topRight.y * height),
            bottomRight = PointValue(quad.bottomRight.x * width, quad.bottomRight.y * height),
            bottomLeft = PointValue(quad.bottomLeft.x * width, quad.bottomLeft.y * height),
        )

    private fun rectToQuadPixels(rect: Rect): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue(rect.left.toFloat(), rect.top.toFloat()),
            topRight = PointValue(rect.right.toFloat(), rect.top.toFloat()),
            bottomRight = PointValue(rect.right.toFloat(), rect.bottom.toFloat()),
            bottomLeft = PointValue(rect.left.toFloat(), rect.bottom.toFloat()),
        )

    private fun rectToNormalizedQuad(
        rect: Rect,
        width: Int,
        height: Int,
    ): DocumentQuad =
        normalizeQuad(rectToQuadPixels(rect), width, height)

    private fun normalizeQuad(
        quad: DocumentQuad,
        width: Int,
        height: Int,
    ): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue((quad.topLeft.x / width).coerceIn(0f, 1f), (quad.topLeft.y / height).coerceIn(0f, 1f)),
            topRight = PointValue((quad.topRight.x / width).coerceIn(0f, 1f), (quad.topRight.y / height).coerceIn(0f, 1f)),
            bottomRight = PointValue((quad.bottomRight.x / width).coerceIn(0f, 1f), (quad.bottomRight.y / height).coerceIn(0f, 1f)),
            bottomLeft = PointValue((quad.bottomLeft.x / width).coerceIn(0f, 1f), (quad.bottomLeft.y / height).coerceIn(0f, 1f)),
        )

    private fun expandRect(
        rect: Rect,
        width: Int,
        height: Int,
        horizontalFraction: Float,
        verticalFraction: Float,
    ): Rect {
        val expandX = (rect.width() * horizontalFraction).roundToInt()
        val expandY = (rect.height() * verticalFraction).roundToInt()
        return Rect(
            max(rect.left - expandX, 0),
            max(rect.top - expandY, 0),
            min(rect.right + expandX, width - 1),
            min(rect.bottom + expandY, height - 1),
        )
    }

    private fun fitVerticalLine(samples: List<BoundarySample>): VerticalLine? {
        if (samples.size < 3) return null
        val weights = samples.map { max(it.score, 1f) }
        val weightSum = weights.sum().coerceAtLeast(1f)
        val meanY = samples.zip(weights).sumOf { (sample, weight) -> sample.anchor * weight.toDouble() }.toFloat() / weightSum
        val meanX = samples.zip(weights).sumOf { (sample, weight) -> sample.value * weight.toDouble() }.toFloat() / weightSum
        var covariance = 0f
        var variance = 0f
        samples.zip(weights).forEach { (sample, weight) ->
            val deltaY = sample.anchor - meanY
            val deltaX = sample.value - meanX
            covariance += weight * deltaY * deltaX
            variance += weight * deltaY * deltaY
        }
        if (variance <= 1f) return null
        val slope = covariance / variance
        return VerticalLine(
            slope = slope,
            intercept = meanX - slope * meanY,
        )
    }

    private fun fitHorizontalLine(samples: List<BoundarySample>): HorizontalLine? {
        if (samples.size < 3) return null
        val weights = samples.map { max(it.score, 1f) }
        val weightSum = weights.sum().coerceAtLeast(1f)
        val meanX = samples.zip(weights).sumOf { (sample, weight) -> sample.anchor * weight.toDouble() }.toFloat() / weightSum
        val meanY = samples.zip(weights).sumOf { (sample, weight) -> sample.value * weight.toDouble() }.toFloat() / weightSum
        var covariance = 0f
        var variance = 0f
        samples.zip(weights).forEach { (sample, weight) ->
            val deltaX = sample.anchor - meanX
            val deltaY = sample.value - meanY
            covariance += weight * deltaX * deltaY
            variance += weight * deltaX * deltaX
        }
        if (variance <= 1f) return null
        val slope = covariance / variance
        return HorizontalLine(
            slope = slope,
            intercept = meanY - slope * meanX,
        )
    }

    private fun intersect(
        vertical: VerticalLine,
        horizontal: HorizontalLine,
    ): PointValue {
        val denominator = 1f - (horizontal.slope * vertical.slope)
        if (abs(denominator) < 0.0001f) {
            return PointValue(vertical.intercept, horizontal.intercept)
        }
        val y = (horizontal.slope * vertical.intercept + horizontal.intercept) / denominator
        return PointValue(vertical.xAt(y), y)
    }

    private fun clampPoint(
        point: PointValue,
        width: Int,
        height: Int,
    ): PointValue =
        PointValue(
            x = point.x.coerceIn(0f, (width - 1).toFloat()),
            y = point.y.coerceIn(0f, (height - 1).toFloat()),
        )

    private fun blendQuad(
        fallback: DocumentQuad,
        candidate: DocumentQuad,
        candidateWeight: Float,
    ): DocumentQuad {
        val fallbackWeight = 1f - candidateWeight
        fun blend(a: PointValue, b: PointValue): PointValue =
            PointValue(
                x = a.x * fallbackWeight + b.x * candidateWeight,
                y = a.y * fallbackWeight + b.y * candidateWeight,
            )
        return DocumentQuad(
            topLeft = blend(fallback.topLeft, candidate.topLeft),
            topRight = blend(fallback.topRight, candidate.topRight),
            bottomRight = blend(fallback.bottomRight, candidate.bottomRight),
            bottomLeft = blend(fallback.bottomLeft, candidate.bottomLeft),
        )
    }

    private fun tightenQuad(
        quad: DocumentQuad,
        amount: Float,
    ): DocumentQuad {
        val centerX = (quad.topLeft.x + quad.topRight.x + quad.bottomRight.x + quad.bottomLeft.x) / 4f
        val centerY = (quad.topLeft.y + quad.topRight.y + quad.bottomRight.y + quad.bottomLeft.y) / 4f

        fun tighten(point: PointValue): PointValue =
            PointValue(
                x = point.x + (centerX - point.x) * amount,
                y = point.y + (centerY - point.y) * amount,
            )

        return DocumentQuad(
            topLeft = tighten(quad.topLeft),
            topRight = tighten(quad.topRight),
            bottomRight = tighten(quad.bottomRight),
            bottomLeft = tighten(quad.bottomLeft),
        )
    }

    private fun isReasonableQuad(
        quad: DocumentQuad,
        width: Int,
        height: Int,
    ): Boolean {
        val area = polygonArea(quad)
        if (area < width * height * 0.18f) return false
        val topWidth = distance(quad.topLeft, quad.topRight)
        val bottomWidth = distance(quad.bottomLeft, quad.bottomRight)
        val leftHeight = distance(quad.topLeft, quad.bottomLeft)
        val rightHeight = distance(quad.topRight, quad.bottomRight)
        if (topWidth < width * 0.25f || bottomWidth < width * 0.25f) return false
        if (leftHeight < height * 0.25f || rightHeight < height * 0.25f) return false
        return quad.topLeft.y < quad.bottomLeft.y &&
            quad.topRight.y < quad.bottomRight.y &&
            quad.topLeft.x < quad.topRight.x &&
            quad.bottomLeft.x < quad.bottomRight.x
    }

    private fun polygonArea(quad: DocumentQuad): Float {
        val points = quad.asList()
        var area = 0f
        for (index in points.indices) {
            val current = points[index]
            val next = points[(index + 1) % points.size]
            area += current.x * next.y - next.x * current.y
        }
        return abs(area) * 0.5f
    }

    private fun distance(
        start: PointValue,
        end: PointValue,
    ): Float = hypot(abs(end.x - start.x), abs(end.y - start.y))

    private data class BoundarySample(
        val anchor: Float,
        val value: Float,
        val score: Float,
    )

    private data class VerticalLine(
        val slope: Float,
        val intercept: Float,
    ) {
        fun xAt(y: Float): Float = slope * y + intercept
    }

    private data class HorizontalLine(
        val slope: Float,
        val intercept: Float,
    )
}
