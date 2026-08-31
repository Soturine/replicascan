package com.soturine.replicascan.core.common.image

import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class DocumentDetectionMetricSample(
    val id: String,
    val expectedDocument: Boolean,
    val expectedQuad: DocumentQuad?,
    val actualDocument: Boolean,
    val actualQuad: DocumentQuad?,
    val latencyMillis: Long,
)

data class DocumentDetectionMetricReport(
    val sampleCount: Int,
    val documentRecall: Float,
    val falsePositiveRate: Float,
    val meanBoundingBoxIou: Float,
    val meanNormalizedCornerError: Float,
    val p95LatencyMillis: Long,
)

/** Deterministic metrics shared by the local fixture harness and CI regression tests. */
object DocumentDetectionMetrics {
    fun report(samples: List<DocumentDetectionMetricSample>): DocumentDetectionMetricReport {
        if (samples.isEmpty()) return DocumentDetectionMetricReport(0, 0f, 0f, 0f, 0f, 0L)
        val positives = samples.filter { it.expectedDocument }
        val negatives = samples.filterNot { it.expectedDocument }
        val matched = positives.filter { it.actualDocument }
        val comparable = matched.filter { it.expectedQuad != null && it.actualQuad != null }
        val sortedLatencies = samples.map { it.latencyMillis }.sorted()
        val p95Index = ((sortedLatencies.size - 1) * 0.95f).toInt()
        return DocumentDetectionMetricReport(
            sampleCount = samples.size,
            documentRecall = matched.size.safeRatio(positives.size),
            falsePositiveRate = negatives.count { it.actualDocument }.safeRatio(negatives.size),
            meanBoundingBoxIou = comparable.map { boundingBoxIou(it.expectedQuad!!, it.actualQuad!!) }.averageOrZero(),
            meanNormalizedCornerError = comparable.map { cornerError(it.expectedQuad!!, it.actualQuad!!) }.averageOrZero(),
            p95LatencyMillis = sortedLatencies[p95Index],
        )
    }

    fun boundingBoxIou(expected: DocumentQuad, actual: DocumentQuad): Float {
        val a = expected.bounds()
        val b = actual.bounds()
        val intersection = max(0f, min(a.right, b.right) - max(a.left, b.left)) *
            max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
        val union = a.area + b.area - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    fun cornerError(expected: DocumentQuad, actual: DocumentQuad): Float =
        expected.asList().zip(actual.asList()).map { (a, b) -> hypot(a.x - b.x, a.y - b.y) }.averageOrZero()

    private data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val area: Float get() = max(0f, right - left) * max(0f, bottom - top)
    }

    private fun DocumentQuad.bounds(): Bounds {
        val points = asList()
        return Bounds(
            left = points.minOf(PointValue::x),
            top = points.minOf(PointValue::y),
            right = points.maxOf(PointValue::x),
            bottom = points.maxOf(PointValue::y),
        )
    }

    private fun Int.safeRatio(denominator: Int): Float = if (denominator == 0) 0f else toFloat() / denominator
    private fun List<Float>.averageOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}
