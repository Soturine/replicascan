package com.soturine.scanora.core.common.image

import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.common.model.DocumentDetectionConfidence
import com.soturine.scanora.core.common.model.DocumentDetectionResult
import com.soturine.scanora.core.common.model.DocumentDetectionStatus
import com.soturine.scanora.core.common.model.DocumentProfile
import com.soturine.scanora.core.common.model.DocumentQuad
import com.soturine.scanora.core.common.model.PointValue
import org.junit.Test

class AutomaticDocumentProfileRouterTest {
    @Test
    fun `routes narrow documents to receipt profile`() {
        assertThat(AutomaticDocumentProfileRouter.route(result(0.36f, 0.86f)))
            .isEqualTo(DocumentProfile.RECEIPT)
    }

    @Test
    fun `keeps ordinary pages in general profile`() {
        assertThat(AutomaticDocumentProfileRouter.route(result(0.68f, 0.82f)))
            .isEqualTo(DocumentProfile.GENERAL)
    }

    @Test
    fun `keeps no detection conservative`() {
        assertThat(AutomaticDocumentProfileRouter.route(DocumentDetectionResult.noDocument()))
            .isEqualTo(DocumentProfile.GENERAL)
    }

    private fun result(width: Float, height: Float) = DocumentDetectionResult(
        status = DocumentDetectionStatus.DETECTED,
        confidence = DocumentDetectionConfidence.HIGH,
        quad = DocumentQuad(
            topLeft = PointValue(0.1f, 0.08f),
            topRight = PointValue(0.1f + width, 0.08f),
            bottomRight = PointValue(0.1f + width, 0.08f + height),
            bottomLeft = PointValue(0.1f, 0.08f + height),
        ),
        score = 0.9f,
        candidateCount = 1,
        usedFallback = false,
        processingTimeMillis = 1L,
    )
}
