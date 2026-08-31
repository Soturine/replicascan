package com.soturine.replicascan.core.data.image.detection

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.soturine.replicascan.core.common.image.DocumentQuadValidator
import com.soturine.replicascan.core.common.model.DocumentDetectionConfidence
import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.DocumentDetectionStatus
import com.soturine.replicascan.core.common.model.DocumentProfile
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

/**
 * Deterministic, local detector that ranks independent edge, brightness and border-contrast
 * candidates. A full-page rectangle is deliberately not a detection candidate: callers receive
 * [DocumentDetectionStatus.NO_DOCUMENT] and decide whether to offer the conservative fallback.
 */
class HeuristicDocumentDetector : DocumentDetector {
    override fun detect(bitmap: Bitmap, profile: DocumentProfile): DocumentDetectionResult {
        return detectLuma(bitmap.toLuma(), bitmap.width, bitmap.height, profile)
    }

    override fun detectLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile,
    ): DocumentDetectionResult {
        var result = DocumentDetectionResult.noDocument()
        val elapsed = measureNanoTime {
            result = detectMeasured(luma, width, height, profile)
        } / NANOS_PER_MILLI
        return result.copy(processingTimeMillis = elapsed)
    }

    private fun detectMeasured(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile,
    ): DocumentDetectionResult {
        if (width < MIN_SIDE || height < MIN_SIDE) return DocumentDetectionResult.noDocument()
        if (luma.size != width * height) return DocumentDetectionResult.noDocument()
        val projections = buildEdgeProjections(luma, width, height)
        val config = ProfileConfig.forProfile(profile)
        val candidates = buildList {
            edgeCandidates(projections, width, height).forEach(::add)
            brightnessCandidate(luma, width, height)?.let(::add)
            borderContrastCandidate(luma, width, height)?.let(::add)
        }
            .map { it.stabilize(width, height) }
            .filter { it.isPlausible(width, height, config) }
            .distinctBy { listOf(it.left / 8, it.top / 8, it.right / 8, it.bottom / 8) }

        val ranked = candidates.mapNotNull { bounds ->
            val estimate = PerspectiveQuadEstimator.estimate(
                luma = luma,
                width = width,
                height = height,
                coarse = PerspectiveQuadEstimator.Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                minimumAreaRatio = config.minimumAreaRatio,
            )
            val quad = estimate?.quad ?: refineAxisAlignedQuad(bounds, projections, width, height)
            if (!DocumentQuadValidator.isValidNormalized(quad, config.minimumAreaRatio)) return@mapNotNull null
            ScoredQuad(
                quad = quad,
                score = score(
                    quad = quad,
                    bounds = bounds,
                    luma = luma,
                    projections = projections,
                    width = width,
                    height = height,
                    config = config,
                    perspectiveSupport = estimate?.support ?: 0f,
                ),
            )
        }.sortedByDescending(ScoredQuad::score)

        val best = ranked.firstOrNull() ?: return DocumentDetectionResult.noDocument()
        val confidence = when {
            best.score >= config.highConfidence -> DocumentDetectionConfidence.HIGH
            best.score >= config.mediumConfidence -> DocumentDetectionConfidence.MEDIUM
            best.score >= config.lowConfidence -> DocumentDetectionConfidence.LOW
            else -> DocumentDetectionConfidence.NONE
        }
        if (confidence == DocumentDetectionConfidence.NONE) {
            return DocumentDetectionResult.noDocument().copy(
                score = best.score,
                candidateCount = ranked.size,
            )
        }
        return DocumentDetectionResult(
            status = if (confidence == DocumentDetectionConfidence.HIGH) {
                DocumentDetectionStatus.DETECTED
            } else {
                DocumentDetectionStatus.REVIEW_REQUIRED
            },
            confidence = confidence,
            quad = best.quad,
            score = best.score,
            candidateCount = ranked.size,
            usedFallback = false,
            processingTimeMillis = 0L,
        )
    }

    private fun buildEdgeProjections(luma: IntArray, width: Int, height: Int): EdgeProjections {
        val rows = FloatArray(height)
        val columns = FloatArray(width)
        var total = 0f
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val horizontal = abs(luma[index + 1] - luma[index - 1])
                val vertical = abs(luma[index + width] - luma[index - width])
                val edge = (horizontal + vertical).toFloat()
                if (edge < MIN_EDGE) continue
                rows[y] += edge
                columns[x] += edge
                total += edge
                count++
            }
        }
        return EdgeProjections(
            rows = rows.smooth(max(height / 120, 2)),
            columns = columns.smooth(max(width / 120, 2)),
            meanEdge = if (count == 0) 0f else total / count,
        )
    }

    private fun edgeCandidates(edges: EdgeProjections, width: Int, height: Int): List<Rect> {
        if (edges.meanEdge < MIN_EDGE) return emptyList()
        return EDGE_BOUNDARY_FRACTIONS.mapNotNull { fraction ->
            val left = edges.columns.boundary(fromStart = true, thresholdFraction = fraction) ?: return@mapNotNull null
            val right = edges.columns.boundary(fromStart = false, thresholdFraction = fraction) ?: return@mapNotNull null
            val top = edges.rows.boundary(fromStart = true, thresholdFraction = fraction) ?: return@mapNotNull null
            val bottom = edges.rows.boundary(fromStart = false, thresholdFraction = fraction) ?: return@mapNotNull null
            Rect(left, top, right, bottom).takeIf { it.width() > 0 && it.height() > 0 }
        }
    }

    private fun brightnessCandidate(luma: IntArray, width: Int, height: Int): Rect? {
        val rows = FloatArray(height)
        val columns = FloatArray(width)
        for (y in 0 until height) {
            var rowTotal = 0L
            for (x in 0 until width) {
                val value = luma[y * width + x]
                rowTotal += value
                columns[x] += value.toFloat()
            }
            rows[y] = rowTotal.toFloat() / width
        }
        for (x in columns.indices) columns[x] /= height
        val border = buildList {
            addAll(rows.take(max(height / 16, 1)))
            addAll(rows.takeLast(max(height / 16, 1)))
            addAll(columns.take(max(width / 16, 1)))
            addAll(columns.takeLast(max(width / 16, 1)))
        }.average().toFloat()
        val center = averageRegion(luma, width, height, width / 4, height / 4, width * 3 / 4, height * 3 / 4)
        if (abs(center - border) < MIN_REGION_CONTRAST) return null
        val documentIsBrighter = center > border
        val threshold = (border + center) / 2f
        return Rect(
            columns.transitionBoundary(threshold, documentIsBrighter, true) ?: return null,
            rows.transitionBoundary(threshold, documentIsBrighter, true) ?: return null,
            columns.transitionBoundary(threshold, documentIsBrighter, false) ?: return null,
            rows.transitionBoundary(threshold, documentIsBrighter, false) ?: return null,
        )
    }

    private fun borderContrastCandidate(luma: IntArray, width: Int, height: Int): Rect? {
        val border = averageBorder(luma, width, height)
        val columns = FloatArray(width)
        val rows = FloatArray(height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val contrast = abs(luma[y * width + x] - border)
                if (contrast < MIN_REGION_CONTRAST) continue
                columns[x]++
                rows[y]++
            }
        }
        val left = columns.occupancyBoundary(height * 0.18f, true) ?: return null
        val right = columns.occupancyBoundary(height * 0.18f, false) ?: return null
        val top = rows.occupancyBoundary(width * 0.18f, true) ?: return null
        val bottom = rows.occupancyBoundary(width * 0.18f, false) ?: return null
        return Rect(left, top, right, bottom)
    }

    private fun refineAxisAlignedQuad(bounds: Rect, edges: EdgeProjections, width: Int, height: Int): DocumentQuad {
        val searchX = max(bounds.width() / 12, 4)
        val searchY = max(bounds.height() / 12, 4)
        val top = edges.rows.peakNear(bounds.top, searchY)
        val bottom = edges.rows.peakNear(bounds.bottom, searchY)
        val left = edges.columns.peakNear(bounds.left, searchX)
        val right = edges.columns.peakNear(bounds.right, searchX)
        return DocumentQuad(
            topLeft = PointValue(left.toFloat() / width, top.toFloat() / height),
            topRight = PointValue(right.toFloat() / width, top.toFloat() / height),
            bottomRight = PointValue(right.toFloat() / width, bottom.toFloat() / height),
            bottomLeft = PointValue(left.toFloat() / width, bottom.toFloat() / height),
        )
    }

    private fun score(
        quad: DocumentQuad,
        bounds: Rect,
        luma: IntArray,
        projections: EdgeProjections,
        width: Int,
        height: Int,
        config: ProfileConfig,
        perspectiveSupport: Float,
    ): Float {
        val areaRatio = bounds.width().toFloat() * bounds.height() / (width.toFloat() * height)
        val aspect = bounds.width().toFloat() / bounds.height().coerceAtLeast(1)
        val areaScore = closeness(areaRatio, config.idealArea, config.areaTolerance)
        val aspectScore = config.aspectScore(aspect)
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val centerDistance = (
            abs(centerX - width / 2f) / width.coerceAtLeast(1) +
                abs(centerY - height / 2f) / height.coerceAtLeast(1)
            )
        val centerScore = (1f - centerDistance * 1.35f).coerceIn(0f, 1f)
        val edgePeak = listOf(
            projections.columns.getOrElse(bounds.left) { 0f } / height,
            projections.columns.getOrElse(bounds.right.coerceAtMost(width - 1)) { 0f } / height,
            projections.rows.getOrElse(bounds.top) { 0f } / width,
            projections.rows.getOrElse(bounds.bottom.coerceAtMost(height - 1)) { 0f } / width,
        ).average().toFloat()
        val edgeScore = (edgePeak / (projections.meanEdge * 0.22f).coerceAtLeast(8f)).coerceIn(0f, 1f)
        val inside = averageRegion(luma, width, height, bounds.left, bounds.top, bounds.right, bounds.bottom)
        val outside = averageBorder(luma, width, height)
        val contrastScore = (abs(inside - outside) / 42f).coerceIn(0f, 1f)
        val geometryScore = if (DocumentQuadValidator.isValidNormalized(quad, config.minimumAreaRatio)) 1f else 0f
        return (
            edgeScore * 0.34f +
                contrastScore * 0.23f +
                areaScore * 0.16f +
                aspectScore * 0.13f +
                centerScore * 0.04f +
                geometryScore * 0.06f +
                perspectiveSupport * 0.04f
            ).coerceIn(0f, 1f)
    }

    private fun Bitmap.toLuma(): IntArray {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val color = pixels[index]
            pixels[index] = (
                Color.red(color) * 77 +
                    Color.green(color) * 150 +
                    Color.blue(color) * 29
                ) shr 8
        }
        return pixels
    }

    private fun FloatArray.smooth(radius: Int): FloatArray {
        val output = FloatArray(size)
        val prefix = FloatArray(size + 1)
        for (index in indices) {
            prefix[index + 1] = prefix[index] + this[index]
        }
        for (index in indices) {
            val start = max(0, index - radius)
            val end = min(lastIndex, index + radius)
            output[index] = (prefix[end + 1] - prefix[start]) / (end - start + 1).coerceAtLeast(1)
        }
        return output
    }

    private fun FloatArray.boundary(fromStart: Boolean, thresholdFraction: Float): Int? {
        val maximum = maxOrNull() ?: return null
        if (maximum <= 0f) return null
        val threshold = maximum * thresholdFraction
        val margin = max(size / 40, 2)
        val range = if (fromStart) margin until size - margin else size - margin - 1 downTo margin
        return range.firstOrNull { this[it] >= threshold }
    }

    private fun FloatArray.transitionBoundary(threshold: Float, above: Boolean, fromStart: Boolean): Int? {
        val margin = max(size / 50, 1)
        val range = if (fromStart) margin until size - margin else size - margin - 1 downTo margin
        return range.firstOrNull { index -> (this[index] >= threshold) == above }
    }

    private fun FloatArray.occupancyBoundary(threshold: Float, fromStart: Boolean): Int? {
        val margin = max(size / 50, 1)
        val range = if (fromStart) margin until size - margin else size - margin - 1 downTo margin
        return range.firstOrNull { this[it] >= threshold }
    }

    private fun FloatArray.peakNear(center: Int, radius: Int): Int {
        val start = max(0, center - radius)
        val end = min(lastIndex, center + radius)
        return (start..end).maxByOrNull { this[it] } ?: center.coerceIn(0, lastIndex)
    }

    private fun Rect.stabilize(width: Int, height: Int): Rect {
        val x = max(width / 300, 2)
        val y = max(height / 300, 2)
        return Rect(
            (left - x).coerceIn(0, width - 1),
            (top - y).coerceIn(0, height - 1),
            (right + x).coerceIn(1, width - 1),
            (bottom + y).coerceIn(1, height - 1),
        )
    }

    private fun Rect.isPlausible(width: Int, height: Int, config: ProfileConfig): Boolean {
        if (width() <= 0 || height() <= 0) return false
        val area = width().toFloat() * height() / (width.toFloat() * height)
        val aspect = width().toFloat() / height().coerceAtLeast(1)
        return area in config.minimumAreaRatio..MAX_AREA_RATIO &&
            aspect in config.minimumAspect..config.maximumAspect
    }

    private fun averageRegion(
        luma: IntArray,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): Float {
        val step = max(min(width, height) / 160, 1)
        var total = 0L
        var count = 0
        for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(height) step step) {
            for (x in left.coerceAtLeast(0) until right.coerceAtMost(width) step step) {
                total += luma[y * width + x]
                count++
            }
        }
        return if (count == 0) 0f else total.toFloat() / count
    }

    private fun averageBorder(luma: IntArray, width: Int, height: Int): Float {
        val insetX = max(width / 28, 1)
        val insetY = max(height / 28, 1)
        val top = averageRegion(luma, width, height, 0, 0, width, insetY)
        val bottom = averageRegion(luma, width, height, 0, height - insetY, width, height)
        val left = averageRegion(luma, width, height, 0, insetY, insetX, height - insetY)
        val right = averageRegion(luma, width, height, width - insetX, insetY, width, height - insetY)
        return (top + bottom + left + right) / 4f
    }

    private fun closeness(value: Float, ideal: Float, tolerance: Float): Float =
        (1f - abs(value - ideal) / tolerance.coerceAtLeast(0.01f)).coerceIn(0f, 1f)

    private data class EdgeProjections(
        val rows: FloatArray,
        val columns: FloatArray,
        val meanEdge: Float,
    )

    private data class ScoredQuad(val quad: DocumentQuad, val score: Float)

    private data class ProfileConfig(
        val minimumAreaRatio: Float,
        val idealArea: Float,
        val areaTolerance: Float,
        val minimumAspect: Float,
        val maximumAspect: Float,
        val idealAspects: List<Float>,
        val highConfidence: Float,
        val mediumConfidence: Float,
        val lowConfidence: Float,
    ) {
        fun aspectScore(aspect: Float): Float = idealAspects.maxOf { ideal ->
            (1f - abs(aspect - ideal) / max(ideal * 0.75f, 0.35f)).coerceIn(0f, 1f)
        }

        companion object {
            fun forProfile(profile: DocumentProfile): ProfileConfig = when (profile) {
                DocumentProfile.GENERAL -> ProfileConfig(0.16f, 0.60f, 0.42f, 0.28f, 3.4f, listOf(0.71f, 1.41f), 0.70f, 0.55f, 0.43f)
                DocumentProfile.NOTEBOOK -> ProfileConfig(0.20f, 0.66f, 0.40f, 0.42f, 2.4f, listOf(0.71f, 1.41f), 0.68f, 0.53f, 0.41f)
                DocumentProfile.RECEIPT -> ProfileConfig(0.07f, 0.34f, 0.31f, 0.16f, 6.8f, listOf(0.32f, 3.1f), 0.65f, 0.50f, 0.38f)
            }
        }
    }

    companion object {
        private const val MIN_SIDE = 96
        private const val MIN_EDGE = 12f
        private const val MIN_REGION_CONTRAST = 9f
        private const val MAX_AREA_RATIO = 0.965f
        private const val NANOS_PER_MILLI = 1_000_000L
        private val EDGE_BOUNDARY_FRACTIONS = listOf(0.18f, 0.27f, 0.36f)
    }
}
