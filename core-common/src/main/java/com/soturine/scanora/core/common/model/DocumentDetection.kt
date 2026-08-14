package com.soturine.scanora.core.common.model

enum class DocumentProfile {
    GENERAL,
    NOTEBOOK,
    RECEIPT,
}

enum class DocumentDetectionStatus {
    DETECTED,
    REVIEW_REQUIRED,
    NO_DOCUMENT,
}

enum class DocumentDetectionConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NONE,
}

data class DocumentDetectionResult(
    val status: DocumentDetectionStatus,
    val confidence: DocumentDetectionConfidence,
    val quad: DocumentQuad?,
    val score: Float,
    val candidateCount: Int,
    val usedFallback: Boolean,
    val processingTimeMillis: Long,
) {
    val requiresReview: Boolean
        get() = status != DocumentDetectionStatus.DETECTED

    fun quadOrFullPage(): DocumentQuad = quad ?: FULL_PAGE_QUAD

    companion object {
        val FULL_PAGE_QUAD = DocumentQuad(
            topLeft = PointValue(0f, 0f),
            topRight = PointValue(1f, 0f),
            bottomRight = PointValue(1f, 1f),
            bottomLeft = PointValue(0f, 1f),
        )

        fun noDocument(processingTimeMillis: Long = 0L): DocumentDetectionResult =
            DocumentDetectionResult(
                status = DocumentDetectionStatus.NO_DOCUMENT,
                confidence = DocumentDetectionConfidence.NONE,
                quad = null,
                score = 0f,
                candidateCount = 0,
                usedFallback = true,
                processingTimeMillis = processingTimeMillis,
            )
    }
}
