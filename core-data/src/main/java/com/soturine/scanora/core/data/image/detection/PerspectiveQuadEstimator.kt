package com.soturine.scanora.core.data.image.detection

import com.soturine.scanora.core.common.image.DocumentQuadValidator
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PointValue
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Fits the four document sides independently around coarse bounds and intersects the resulting
 * lines. The implementation is deliberately platform-free so its geometry can be regression
 * tested on the JVM without decoding Android bitmaps.
 */
internal object PerspectiveQuadEstimator {
    data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    data class Estimate(
        val quad: DocumentQuad,
        val support: Float,
        val meanBoundaryScore: Float,
    )

    fun estimate(
        luma: IntArray,
        width: Int,
        height: Int,
        coarse: Bounds,
        minimumAreaRatio: Float,
    ): Estimate? {
        if (width < 32 || height < 32 || luma.size != width * height) return null
        if (coarse.width <= 0 || coarse.height <= 0) return null

        val edges = edgeMagnitude(luma, width, height)
        val threshold = strongEdgeThreshold(edges)
        val expandX = max((coarse.width * 0.20f).toInt(), 8)
        val expandY = max((coarse.height * 0.20f).toInt(), 8)
        val searchLeft = max(2, coarse.left - expandX)
        val searchRight = min(width - 3, coarse.right + expandX)
        val searchTop = max(2, coarse.top - expandY)
        val searchBottom = min(height - 3, coarse.bottom + expandY)
        val horizontalBand = max(coarse.width / 3, width / 10)
        val verticalBand = max(coarse.height / 3, height / 10)

        val left = verticalSamples(
            luma, edges, width, height,
            coarse.top, coarse.bottom,
            searchLeft, min(searchRight, coarse.left + horizontalBand),
            coarse.left, insidePositive = true, threshold,
        )
        val right = verticalSamples(
            luma, edges, width, height,
            coarse.top, coarse.bottom,
            max(searchLeft, coarse.right - horizontalBand), searchRight,
            coarse.right, insidePositive = false, threshold,
        )
        val top = horizontalSamples(
            luma, edges, width, height,
            coarse.left, coarse.right,
            searchTop, min(searchBottom, coarse.top + verticalBand),
            coarse.top, insidePositive = true, threshold,
        )
        val bottom = horizontalSamples(
            luma, edges, width, height,
            coarse.left, coarse.right,
            max(searchTop, coarse.bottom - verticalBand), searchBottom,
            coarse.bottom, insidePositive = false, threshold,
        )

        val leftLine = fitVerticalRobust(left) ?: return null
        val rightLine = fitVerticalRobust(right) ?: return null
        val topLine = fitHorizontalRobust(top) ?: return null
        val bottomLine = fitHorizontalRobust(bottom) ?: return null
        if (abs(leftLine.slope) > MAX_SIDE_SLOPE || abs(rightLine.slope) > MAX_SIDE_SLOPE) return null
        if (abs(topLine.slope) > MAX_SIDE_SLOPE || abs(bottomLine.slope) > MAX_SIDE_SLOPE) return null

        val pixelQuad = DocumentQuad(
            topLeft = intersect(leftLine, topLine),
            topRight = intersect(rightLine, topLine),
            bottomRight = intersect(rightLine, bottomLine),
            bottomLeft = intersect(leftLine, bottomLine),
        ).clamp(width, height)
        val normalized = pixelQuad.normalize(width, height)
        if (!DocumentQuadValidator.isValidNormalized(normalized, minimumAreaRatio)) return null
        if (!isReasonable(normalized, minimumAreaRatio)) return null

        val samples = left + right + top + bottom
        val expectedSamples = expectedSampleCount(coarse.width) * 2 + expectedSampleCount(coarse.height) * 2
        return Estimate(
            quad = normalized,
            support = (samples.size.toFloat() / expectedSamples.coerceAtLeast(1)).coerceIn(0f, 1f),
            meanBoundaryScore = samples.map(BoundarySample::score).average().toFloat().coerceAtLeast(0f),
        )
    }

    private fun edgeMagnitude(luma: IntArray, width: Int, height: Int): FloatArray {
        val output = FloatArray(luma.size)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                val dx = abs(luma[index + 1] - luma[index - 1])
                val dy = abs(luma[index + width] - luma[index - width])
                output[index] = (dx + dy).toFloat()
            }
        }
        return output
    }

    private fun strongEdgeThreshold(edges: FloatArray): Float {
        val histogram = IntArray(128)
        edges.forEach { value ->
            if (value > 0f) histogram[(value / 4f).toInt().coerceIn(histogram.indices)]++
        }
        val populated = histogram.sum()
        if (populated == 0) return MIN_ACCEPTED_EDGE
        val target = (populated * 0.78f).toInt()
        var cumulative = 0
        for (index in histogram.indices) {
            cumulative += histogram[index]
            if (cumulative >= target) return max(MIN_ACCEPTED_EDGE, index * 4f)
        }
        return MIN_ACCEPTED_EDGE
    }

    private fun verticalSamples(
        luma: IntArray,
        edges: FloatArray,
        width: Int,
        height: Int,
        anchorStart: Int,
        anchorEnd: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insidePositive: Boolean,
        threshold: Float,
    ): List<BoundarySample> {
        val step = max((anchorEnd - anchorStart) / SAMPLE_DIVISIONS, MIN_SAMPLE_STEP)
        if (searchEnd <= searchStart || anchorEnd - anchorStart < step * 2) return emptyList()
        return buildList {
            var y = anchorStart + step / 2
            while (y < anchorEnd - step / 2) {
                bestVerticalSample(
                    luma, edges, width, height, y,
                    searchStart, searchEnd, expected, insidePositive, threshold,
                )?.let(::add)
                y += step
            }
        }
    }

    private fun horizontalSamples(
        luma: IntArray,
        edges: FloatArray,
        width: Int,
        height: Int,
        anchorStart: Int,
        anchorEnd: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insidePositive: Boolean,
        threshold: Float,
    ): List<BoundarySample> {
        val step = max((anchorEnd - anchorStart) / SAMPLE_DIVISIONS, MIN_SAMPLE_STEP)
        if (searchEnd <= searchStart || anchorEnd - anchorStart < step * 2) return emptyList()
        return buildList {
            var x = anchorStart + step / 2
            while (x < anchorEnd - step / 2) {
                bestHorizontalSample(
                    luma, edges, width, height, x,
                    searchStart, searchEnd, expected, insidePositive, threshold,
                )?.let(::add)
                x += step
            }
        }
    }

    private fun bestVerticalSample(
        luma: IntArray,
        edges: FloatArray,
        width: Int,
        height: Int,
        y: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insidePositive: Boolean,
        threshold: Float,
    ): BoundarySample? {
        val safeY = y.coerceIn(2, height - 3)
        val start = searchStart.coerceIn(2, width - 3)
        val end = searchEnd.coerceIn(2, width - 3)
        if (end <= start) return null
        var best: BoundarySample? = null
        for (x in start..end) {
            val gradient = abs(luma[safeY * width + x + 2] - luma[safeY * width + x - 2]).toFloat()
            val inside = horizontalAverage(luma, width, height, x, safeY, if (insidePositive) 2 else -9, if (insidePositive) 9 else -2)
            val outside = horizontalAverage(luma, width, height, x, safeY, if (insidePositive) -9 else 2, if (insidePositive) -2 else 9)
            val contrast = abs(inside - outside)
            val distancePenalty = abs(x - expected).toFloat() / (end - start).coerceAtLeast(1) * 10f
            val score = gradient * 0.56f + edges[safeY * width + x] * 0.36f + contrast * 0.50f - distancePenalty
            if (score >= max(MIN_ACCEPTED_EDGE, threshold * 0.62f) && (best == null || score > best.score)) {
                best = BoundarySample(safeY.toFloat(), x.toFloat(), score)
            }
        }
        return best
    }

    private fun bestHorizontalSample(
        luma: IntArray,
        edges: FloatArray,
        width: Int,
        height: Int,
        x: Int,
        searchStart: Int,
        searchEnd: Int,
        expected: Int,
        insidePositive: Boolean,
        threshold: Float,
    ): BoundarySample? {
        val safeX = x.coerceIn(2, width - 3)
        val start = searchStart.coerceIn(2, height - 3)
        val end = searchEnd.coerceIn(2, height - 3)
        if (end <= start) return null
        var best: BoundarySample? = null
        for (y in start..end) {
            val gradient = abs(luma[(y + 2) * width + safeX] - luma[(y - 2) * width + safeX]).toFloat()
            val inside = verticalAverage(luma, width, height, safeX, y, if (insidePositive) 2 else -9, if (insidePositive) 9 else -2)
            val outside = verticalAverage(luma, width, height, safeX, y, if (insidePositive) -9 else 2, if (insidePositive) -2 else 9)
            val contrast = abs(inside - outside)
            val distancePenalty = abs(y - expected).toFloat() / (end - start).coerceAtLeast(1) * 10f
            val score = gradient * 0.56f + edges[y * width + safeX] * 0.36f + contrast * 0.50f - distancePenalty
            if (score >= max(MIN_ACCEPTED_EDGE, threshold * 0.62f) && (best == null || score > best.score)) {
                best = BoundarySample(safeX.toFloat(), y.toFloat(), score)
            }
        }
        return best
    }

    private fun horizontalAverage(luma: IntArray, width: Int, height: Int, x: Int, y: Int, start: Int, end: Int): Float {
        var total = 0
        var count = 0
        for (offset in min(start, end)..max(start, end)) {
            val sampleX = (x + offset).coerceIn(0, width - 1)
            total += luma[y.coerceIn(0, height - 1) * width + sampleX]
            count++
        }
        return total.toFloat() / count.coerceAtLeast(1)
    }

    private fun verticalAverage(luma: IntArray, width: Int, height: Int, x: Int, y: Int, start: Int, end: Int): Float {
        var total = 0
        var count = 0
        for (offset in min(start, end)..max(start, end)) {
            val sampleY = (y + offset).coerceIn(0, height - 1)
            total += luma[sampleY * width + x.coerceIn(0, width - 1)]
            count++
        }
        return total.toFloat() / count.coerceAtLeast(1)
    }

    private fun fitVerticalRobust(samples: List<BoundarySample>): VerticalLine? {
        val first = fitVertical(samples) ?: return null
        val residuals = samples.map { abs(it.value - first.xAt(it.anchor)) }.sorted()
        val median = residuals[residuals.size / 2]
        return fitVertical(samples.filter { abs(it.value - first.xAt(it.anchor)) <= max(3.5f, median * 2.5f) })
    }

    private fun fitHorizontalRobust(samples: List<BoundarySample>): HorizontalLine? {
        val first = fitHorizontal(samples) ?: return null
        val residuals = samples.map { abs(it.value - first.yAt(it.anchor)) }.sorted()
        val median = residuals[residuals.size / 2]
        return fitHorizontal(samples.filter { abs(it.value - first.yAt(it.anchor)) <= max(3.5f, median * 2.5f) })
    }

    private fun fitVertical(samples: List<BoundarySample>): VerticalLine? {
        if (samples.size < MIN_LINE_SAMPLES) return null
        val weights = samples.map { max(it.score, 1f) }
        val sum = weights.sum().coerceAtLeast(1f)
        val meanY = samples.indices.sumOf { samples[it].anchor * weights[it].toDouble() }.toFloat() / sum
        val meanX = samples.indices.sumOf { samples[it].value * weights[it].toDouble() }.toFloat() / sum
        var covariance = 0f
        var variance = 0f
        samples.indices.forEach { index ->
            val dy = samples[index].anchor - meanY
            covariance += weights[index] * dy * (samples[index].value - meanX)
            variance += weights[index] * dy * dy
        }
        if (variance <= 1f) return null
        val slope = covariance / variance
        return VerticalLine(slope, meanX - slope * meanY)
    }

    private fun fitHorizontal(samples: List<BoundarySample>): HorizontalLine? {
        if (samples.size < MIN_LINE_SAMPLES) return null
        val weights = samples.map { max(it.score, 1f) }
        val sum = weights.sum().coerceAtLeast(1f)
        val meanX = samples.indices.sumOf { samples[it].anchor * weights[it].toDouble() }.toFloat() / sum
        val meanY = samples.indices.sumOf { samples[it].value * weights[it].toDouble() }.toFloat() / sum
        var covariance = 0f
        var variance = 0f
        samples.indices.forEach { index ->
            val dx = samples[index].anchor - meanX
            covariance += weights[index] * dx * (samples[index].value - meanY)
            variance += weights[index] * dx * dx
        }
        if (variance <= 1f) return null
        val slope = covariance / variance
        return HorizontalLine(slope, meanY - slope * meanX)
    }

    private fun intersect(vertical: VerticalLine, horizontal: HorizontalLine): PointValue {
        val denominator = 1f - horizontal.slope * vertical.slope
        if (abs(denominator) < 0.0001f) return PointValue(vertical.intercept, horizontal.intercept)
        val y = (horizontal.slope * vertical.intercept + horizontal.intercept) / denominator
        return PointValue(vertical.xAt(y), y)
    }

    private fun DocumentQuad.clamp(width: Int, height: Int): DocumentQuad {
        fun PointValue.clamped() = PointValue(x.coerceIn(0f, width - 1f), y.coerceIn(0f, height - 1f))
        return DocumentQuad(topLeft.clamped(), topRight.clamped(), bottomRight.clamped(), bottomLeft.clamped())
    }

    private fun DocumentQuad.normalize(width: Int, height: Int): DocumentQuad {
        fun PointValue.normalized() = PointValue(x / width.toFloat(), y / height.toFloat())
        return DocumentQuad(topLeft.normalized(), topRight.normalized(), bottomRight.normalized(), bottomLeft.normalized())
    }

    private fun isReasonable(quad: DocumentQuad, minimumAreaRatio: Float): Boolean {
        val points = quad.asList()
        var area = 0f
        points.indices.forEach { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            area += current.x * next.y - next.x * current.y
        }
        if (abs(area) * 0.5f < minimumAreaRatio) return false
        val top = distance(quad.topLeft, quad.topRight)
        val bottom = distance(quad.bottomLeft, quad.bottomRight)
        val left = distance(quad.topLeft, quad.bottomLeft)
        val right = distance(quad.topRight, quad.bottomRight)
        if (min(top, bottom) < 0.18f || min(left, right) < 0.18f) return false
        val widthRatio = max(top, bottom) / min(top, bottom).coerceAtLeast(0.01f)
        val heightRatio = max(left, right) / min(left, right).coerceAtLeast(0.01f)
        return widthRatio <= MAX_OPPOSITE_SIDE_RATIO && heightRatio <= MAX_OPPOSITE_SIDE_RATIO
    }

    private fun distance(first: PointValue, second: PointValue): Float = hypot(second.x - first.x, second.y - first.y)
    private fun expectedSampleCount(span: Int): Int = max(span / max(span / SAMPLE_DIVISIONS, MIN_SAMPLE_STEP), 1)

    private data class BoundarySample(val anchor: Float, val value: Float, val score: Float)
    private data class VerticalLine(val slope: Float, val intercept: Float) { fun xAt(y: Float) = slope * y + intercept }
    private data class HorizontalLine(val slope: Float, val intercept: Float) { fun yAt(x: Float) = slope * x + intercept }

    private const val SAMPLE_DIVISIONS = 14
    private const val MIN_SAMPLE_STEP = 8
    private const val MIN_LINE_SAMPLES = 4
    private const val MIN_ACCEPTED_EDGE = 15f
    private const val MAX_SIDE_SLOPE = 0.72f
    private const val MAX_OPPOSITE_SIDE_RATIO = 2.8f
}
