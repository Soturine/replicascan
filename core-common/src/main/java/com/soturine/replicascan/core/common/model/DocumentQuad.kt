package com.soturine.replicascan.core.common.model

data class PointValue(
    val x: Float,
    val y: Float,
)

data class DocumentQuad(
    val topLeft: PointValue,
    val topRight: PointValue,
    val bottomRight: PointValue,
    val bottomLeft: PointValue,
) {
    fun asList(): List<PointValue> = listOf(topLeft, topRight, bottomRight, bottomLeft)
}

