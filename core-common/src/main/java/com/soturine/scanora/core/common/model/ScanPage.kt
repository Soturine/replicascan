package com.soturine.scanora.core.common.model

data class ScanPage(
    val id: String,
    val scanId: String,
    val index: Int,
    val sourceUri: String,
    val processedUri: String? = null,
    val filterType: DocumentFilterType = DocumentFilterType.AUTO,
    val rotationDegrees: Int = 0,
    val quad: DocumentQuad? = null,
    val ocrText: String? = null,
) {
    val canonicalUri: String
        get() = sourceUri

    val previewUri: String
        get() = processedUri ?: sourceUri

    val displayUri: String
        get() = previewUri
}

