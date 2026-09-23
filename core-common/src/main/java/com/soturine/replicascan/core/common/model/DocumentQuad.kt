package com.soturine.replicascan.core.common.model

data class PointValue(
    val x: Float,
    val y: Float,
)

/** Normalized (0..1) corners of the region the user wants to keep. */
data class DocumentQuad(
    val topLeft: PointValue,
    val topRight: PointValue,
    val bottomRight: PointValue,
    val bottomLeft: PointValue,
) {
    fun asList(): List<PointValue> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    companion object {
        val FULL_PAGE = DocumentQuad(
            topLeft = PointValue(0f, 0f),
            topRight = PointValue(1f, 0f),
            bottomRight = PointValue(1f, 1f),
            bottomLeft = PointValue(0f, 1f),
        )
    }
}
