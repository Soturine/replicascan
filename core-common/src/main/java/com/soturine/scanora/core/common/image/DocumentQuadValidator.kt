package com.soturine.scanora.core.common.image

import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PointValue
import kotlin.math.abs

object DocumentQuadValidator {
    fun isValidNormalized(quad: DocumentQuad, minimumAreaRatio: Float = 0.02f): Boolean =
        isValid(quad, width = 1f, height = 1f, minimumAreaRatio = minimumAreaRatio)

    fun isValid(
        quad: DocumentQuad,
        width: Float,
        height: Float,
        minimumAreaRatio: Float = 0.02f,
    ): Boolean {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return false
        val points = quad.asList()
        if (points.any { !it.x.isFinite() || !it.y.isFinite() || it.x !in 0f..width || it.y !in 0f..height }) return false
        val crosses = points.indices.map { index ->
            cross(points[index], points[(index + 1) % 4], points[(index + 2) % 4])
        }
        if (crosses.any { abs(it) < 0.0001f }) return false
        if (!(crosses.all { it > 0f } || crosses.all { it < 0f })) return false
        return polygonArea(points) >= width * height * minimumAreaRatio.coerceIn(0f, 1f)
    }

    private fun cross(a: PointValue, b: PointValue, c: PointValue): Float =
        (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)

    private fun polygonArea(points: List<PointValue>): Float = abs(
        points.indices.sumOf { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            (current.x * next.y - next.x * current.y).toDouble()
        }.toFloat() / 2f,
    )
}
