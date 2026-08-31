package com.soturine.replicascan.core.common.image

import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import kotlin.math.abs
import kotlin.math.hypot

enum class DocumentQuadFailure {
    INVALID_CANVAS,
    NON_FINITE_POINT,
    POINT_OUT_OF_BOUNDS,
    DEGENERATE_EDGE,
    SELF_INTERSECTION,
    AREA_TOO_SMALL,
    IMPLAUSIBLE_ASPECT_RATIO,
}

data class DocumentQuadValidationResult(
    val isValid: Boolean,
    val failure: DocumentQuadFailure? = null,
    val areaRatio: Float = 0f,
)

object DocumentQuadValidator {
    fun isValidNormalized(quad: DocumentQuad, minimumAreaRatio: Float = 0.02f): Boolean =
        isValid(quad, width = 1f, height = 1f, minimumAreaRatio = minimumAreaRatio)

    fun isValid(
        quad: DocumentQuad,
        width: Float,
        height: Float,
        minimumAreaRatio: Float = 0.02f,
    ): Boolean = validate(quad, width, height, minimumAreaRatio).isValid

    fun validate(
        quad: DocumentQuad,
        width: Float,
        height: Float,
        minimumAreaRatio: Float = 0.02f,
        minimumEdgeRatio: Float = 0.02f,
        allowedAspectRatio: ClosedFloatingPointRange<Float> = 0.15f..6.5f,
    ): DocumentQuadValidationResult {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.INVALID_CANVAS)
        }
        val points = quad.asList()
        if (points.any { !it.x.isFinite() || !it.y.isFinite() }) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.NON_FINITE_POINT)
        }
        if (points.any { it.x !in 0f..width || it.y !in 0f..height }) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.POINT_OUT_OF_BOUNDS)
        }
        val minimumEdge = minOf(width, height) * minimumEdgeRatio.coerceIn(0f, 1f)
        if (points.indices.any { index -> distance(points[index], points[(index + 1) % 4]) < minimumEdge }) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.DEGENERATE_EDGE)
        }
        val crosses = points.indices.map { index ->
            cross(points[index], points[(index + 1) % 4], points[(index + 2) % 4])
        }
        if (crosses.any { abs(it) < 0.0001f } || !(crosses.all { it > 0f } || crosses.all { it < 0f })) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.SELF_INTERSECTION)
        }
        val areaRatio = polygonArea(points) / (width * height)
        if (areaRatio < minimumAreaRatio.coerceIn(0f, 1f)) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.AREA_TOO_SMALL, areaRatio)
        }
        val averageWidth = (distance(points[0], points[1]) + distance(points[2], points[3])) / 2f
        val averageHeight = (distance(points[1], points[2]) + distance(points[3], points[0])) / 2f
        val aspectRatio = averageWidth / averageHeight.coerceAtLeast(0.0001f)
        if (aspectRatio !in allowedAspectRatio) {
            return DocumentQuadValidationResult(false, DocumentQuadFailure.IMPLAUSIBLE_ASPECT_RATIO, areaRatio)
        }
        return DocumentQuadValidationResult(true, areaRatio = areaRatio)
    }

    private fun cross(a: PointValue, b: PointValue, c: PointValue): Float =
        (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)

    private fun distance(a: PointValue, b: PointValue): Float = hypot(a.x - b.x, a.y - b.y)

    private fun polygonArea(points: List<PointValue>): Float = abs(
        points.indices.sumOf { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            (current.x * next.y - next.x * current.y).toDouble()
        }.toFloat() / 2f,
    )
}
