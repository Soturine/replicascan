package com.soturine.replicascan.core.data.image.detection

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import kotlin.math.hypot
import org.junit.Test

class PerspectiveQuadEstimatorTest {
    @Test
    fun `recovers perspective corners across deterministic regression matrix`() {
        val cases = listOf(
            quad(0.16f, 0.13f, 0.84f, 0.16f, 0.80f, 0.88f, 0.19f, 0.84f),
            quad(0.24f, 0.10f, 0.78f, 0.18f, 0.88f, 0.86f, 0.12f, 0.91f),
            quad(0.10f, 0.22f, 0.90f, 0.12f, 0.82f, 0.78f, 0.18f, 0.90f),
            quad(0.20f, 0.18f, 0.72f, 0.10f, 0.85f, 0.90f, 0.13f, 0.82f),
            quad(0.11f, 0.08f, 0.89f, 0.13f, 0.76f, 0.94f, 0.22f, 0.89f),
            quad(0.29f, 0.12f, 0.75f, 0.22f, 0.82f, 0.92f, 0.18f, 0.82f),
        )

        val expandedCases = buildList {
            repeat(3) { textureVariant ->
                cases.forEach { add(it to textureVariant) }
            }
        }
        val metrics = expandedCases.map { (expected, textureVariant) ->
            val fixture = syntheticDocument(expected, textureVariant)
            val bounds = expected.bounds(WIDTH, HEIGHT, padding = 14)
            val estimate = PerspectiveQuadEstimator.estimate(
                luma = fixture,
                width = WIDTH,
                height = HEIGHT,
                coarse = bounds,
                minimumAreaRatio = 0.12f,
            )
            assertThat(estimate).isNotNull()
            val actual = estimate!!.quad
            Metrics(
                meanCornerError = meanCornerError(expected, actual),
                sampledIoU = sampledIoU(expected, actual),
            )
        }

        assertThat(metrics).hasSize(18)
        assertThat(metrics.maxOf(Metrics::meanCornerError)).isLessThan(0.065f)
        assertThat(metrics.minOf(Metrics::sampledIoU)).isGreaterThan(0.84f)
    }

    @Test
    fun `does not invent sides on a flat background`() {
        val luma = IntArray(WIDTH * HEIGHT) { 128 }

        val estimate = PerspectiveQuadEstimator.estimate(
            luma = luma,
            width = WIDTH,
            height = HEIGHT,
            coarse = PerspectiveQuadEstimator.Bounds(40, 50, WIDTH - 40, HEIGHT - 50),
            minimumAreaRatio = 0.12f,
        )

        assertThat(estimate).isNull()
    }

    private fun syntheticDocument(expected: DocumentQuad, textureVariant: Int): IntArray {
        val output = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val normalized = PointValue(x / WIDTH.toFloat(), y / HEIGHT.toFloat())
                val inside = pointInside(expected, normalized)
                output[y * WIDTH + x] = if (inside) {
                    val ruledLine = if ((y + textureVariant * 5) % 29 in 0..1) -34 else 0
                    (224 + ruledLine + ((x + y + textureVariant) % 5)).coerceIn(0, 255)
                } else {
                    (38 + ((x * 3 + y * 5 + textureVariant * 11) % 17)).coerceIn(0, 255)
                }
            }
        }
        return output
    }

    private fun pointInside(quad: DocumentQuad, point: PointValue): Boolean {
        val points = quad.asList()
        var sign = 0f
        points.indices.forEach { index ->
            val first = points[index]
            val second = points[(index + 1) % points.size]
            val cross = (second.x - first.x) * (point.y - first.y) - (second.y - first.y) * (point.x - first.x)
            if (cross != 0f) {
                if (sign == 0f) sign = cross else if (sign * cross < 0f) return false
            }
        }
        return true
    }

    private fun sampledIoU(expected: DocumentQuad, actual: DocumentQuad): Float {
        var intersection = 0
        var union = 0
        for (y in 0 until 120) {
            for (x in 0 until 90) {
                val point = PointValue((x + 0.5f) / 90f, (y + 0.5f) / 120f)
                val inExpected = pointInside(expected, point)
                val inActual = pointInside(actual, point)
                if (inExpected || inActual) union++
                if (inExpected && inActual) intersection++
            }
        }
        return intersection.toFloat() / union.coerceAtLeast(1)
    }

    private fun meanCornerError(expected: DocumentQuad, actual: DocumentQuad): Float =
        expected.asList().zip(actual.asList()).map { (first, second) ->
            hypot(second.x - first.x, second.y - first.y)
        }.average().toFloat()

    private fun DocumentQuad.bounds(width: Int, height: Int, padding: Int): PerspectiveQuadEstimator.Bounds {
        val points = asList()
        return PerspectiveQuadEstimator.Bounds(
            left = (points.minOf(PointValue::x) * width).toInt().minus(padding).coerceAtLeast(0),
            top = (points.minOf(PointValue::y) * height).toInt().minus(padding).coerceAtLeast(0),
            right = (points.maxOf(PointValue::x) * width).toInt().plus(padding).coerceAtMost(width - 1),
            bottom = (points.maxOf(PointValue::y) * height).toInt().plus(padding).coerceAtMost(height - 1),
        )
    }

    private fun quad(
        tlx: Float, tly: Float,
        trx: Float, tryValue: Float,
        brx: Float, bry: Float,
        blx: Float, bly: Float,
    ) = DocumentQuad(
        topLeft = PointValue(tlx, tly),
        topRight = PointValue(trx, tryValue),
        bottomRight = PointValue(brx, bry),
        bottomLeft = PointValue(blx, bly),
    )

    private data class Metrics(val meanCornerError: Float, val sampledIoU: Float)

    private companion object {
        const val WIDTH = 320
        const val HEIGHT = 420
    }
}
