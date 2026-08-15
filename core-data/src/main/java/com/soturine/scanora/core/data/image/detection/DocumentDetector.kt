package com.soturine.scanora.core.data.image.detection

import android.graphics.Bitmap
import com.soturine.scanora.core.common.model.DocumentDetectionResult
import com.soturine.scanora.core.common.model.DocumentProfile

interface DocumentDetector {
    fun detect(bitmap: Bitmap, profile: DocumentProfile): DocumentDetectionResult

    fun detectLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile,
    ): DocumentDetectionResult
}
