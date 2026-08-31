package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.image.DocumentDetectionMetricSample
import com.soturine.replicascan.core.common.image.DocumentDetectionMetrics
import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import org.junit.Test

class DocumentDetectionMetricsTest {
    @Test
    fun identicalQuadHasPerfectIouAndNoCornerError() {
        val quad = DocumentDetectionResult.FULL_PAGE_QUAD
        assertThat(DocumentDetectionMetrics.boundingBoxIou(quad, quad)).isEqualTo(1f)
        assertThat(DocumentDetectionMetrics.cornerError(quad, quad)).isEqualTo(0f)
    }

    @Test
    fun reportSeparatesMissesAndFalsePositives() {
        val expected = DocumentQuad(
            topLeft = PointValue(0.1f, 0.1f), topRight = PointValue(0.9f, 0.1f),
            bottomRight = PointValue(0.9f, 0.9f), bottomLeft = PointValue(0.1f, 0.9f),
        )
        val report = DocumentDetectionMetrics.report(
            listOf(
                DocumentDetectionMetricSample("hit", true, expected, true, expected, 10),
                DocumentDetectionMetricSample("miss", true, expected, false, null, 20),
                DocumentDetectionMetricSample("false-positive", false, null, true, expected, 30),
                DocumentDetectionMetricSample("negative", false, null, false, null, 40),
            ),
        )
        assertThat(report.documentRecall).isEqualTo(0.5f)
        assertThat(report.falsePositiveRate).isEqualTo(0.5f)
        assertThat(report.meanBoundingBoxIou).isEqualTo(1f)
        assertThat(report.p95LatencyMillis).isEqualTo(30L)
    }
}
