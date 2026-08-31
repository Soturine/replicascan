package com.soturine.replicascan.core.common.image

import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.DocumentProfile
import kotlin.math.max
import kotlin.math.min

/** Keeps technical document profiles inside the processing pipeline instead of asking the user. */
object AutomaticDocumentProfileRouter {
    fun route(initial: DocumentDetectionResult): DocumentProfile {
        val quad = initial.quad ?: return DocumentProfile.GENERAL
        val topWidth = quad.topRight.x - quad.topLeft.x
        val bottomWidth = quad.bottomRight.x - quad.bottomLeft.x
        val leftHeight = quad.bottomLeft.y - quad.topLeft.y
        val rightHeight = quad.bottomRight.y - quad.topRight.y
        val width = (topWidth + bottomWidth) / 2f
        val height = (leftHeight + rightHeight) / 2f
        val aspect = max(width, height) / min(width, height).coerceAtLeast(0.01f)
        return if (aspect >= RECEIPT_ASPECT_THRESHOLD) DocumentProfile.RECEIPT else DocumentProfile.GENERAL
    }

    private const val RECEIPT_ASPECT_THRESHOLD = 2.25f
}
