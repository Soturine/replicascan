package com.soturine.replicascan.core.data.image.detection

import android.graphics.Bitmap
import com.soturine.replicascan.core.common.model.DocumentDetectionResult
import com.soturine.replicascan.core.common.model.DocumentProfile

interface DocumentDetector {
    fun detect(bitmap: Bitmap, profile: DocumentProfile): DocumentDetectionResult

    fun detectLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile,
    ): DocumentDetectionResult
}
