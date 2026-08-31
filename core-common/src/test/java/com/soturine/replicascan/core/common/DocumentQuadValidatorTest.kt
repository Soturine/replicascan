package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.image.DocumentQuadValidator
import com.soturine.replicascan.core.common.image.DocumentQuadFailure
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import org.junit.Test

class DocumentQuadValidatorTest {

    @Test
    fun reportsSelfIntersectionInsteadOfAcceptingInvalidCrop() {
        val bowTie = DocumentQuad(
            topLeft = PointValue(0.1f, 0.1f),
            topRight = PointValue(0.9f, 0.9f),
            bottomRight = PointValue(0.9f, 0.1f),
            bottomLeft = PointValue(0.1f, 0.9f),
        )

        assertThat(DocumentQuadValidator.validate(bowTie, 1f, 1f).failure)
            .isEqualTo(DocumentQuadFailure.SELF_INTERSECTION)
    }
    @Test fun acceptsConvexDocument() {
        assertThat(DocumentQuadValidator.isValidNormalized(quad(0.1f, 0.1f, 0.9f, 0.9f))).isTrue()
    }

    @Test fun rejectsCrossedAndTinyDocuments() {
        val crossed = DocumentQuad(
            PointValue(0.1f, 0.1f), PointValue(0.9f, 0.9f),
            PointValue(0.9f, 0.1f), PointValue(0.1f, 0.9f),
        )
        assertThat(DocumentQuadValidator.isValidNormalized(crossed)).isFalse()
        assertThat(DocumentQuadValidator.isValidNormalized(quad(0.49f, 0.49f, 0.51f, 0.51f))).isFalse()
    }

    @Test fun rejectsOutOfBoundsAndNonFinitePoints() {
        assertThat(DocumentQuadValidator.isValidNormalized(quad(-0.1f, 0.1f, 0.9f, 0.9f))).isFalse()
        val invalid = quad(0.1f, 0.1f, 0.9f, 0.9f).copy(topLeft = PointValue(Float.NaN, 0.1f))
        assertThat(DocumentQuadValidator.isValidNormalized(invalid)).isFalse()
    }

    private fun quad(left: Float, top: Float, right: Float, bottom: Float) = DocumentQuad(
        PointValue(left, top), PointValue(right, top), PointValue(right, bottom), PointValue(left, bottom),
    )
}
