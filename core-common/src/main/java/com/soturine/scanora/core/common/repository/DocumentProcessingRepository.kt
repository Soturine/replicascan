package com.soturine.scanora.core.common.repository

import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.DocumentDetectionResult
import com.soturine.scanora.core.common.model.DocumentProfile
import com.soturine.scanora.core.common.model.DocumentQuad

interface DocumentProcessingRepository {
    suspend fun detectDocumentAutomatically(imageUri: String): DocumentDetectionResult =
        detectDocument(imageUri, DocumentProfile.GENERAL)

    suspend fun detectDocument(
        imageUri: String,
        profile: DocumentProfile = DocumentProfile.GENERAL,
    ): DocumentDetectionResult = DocumentDetectionResult.noDocument()

    suspend fun estimateDocumentQuad(imageUri: String): DocumentQuad =
        detectDocument(imageUri).quadOrFullPage()

    /** Lightweight camera-analysis entry point. The luma plane must be tightly packed. */
    suspend fun detectPreviewLuma(
        luma: IntArray,
        width: Int,
        height: Int,
        profile: DocumentProfile = DocumentProfile.GENERAL,
    ): DocumentDetectionResult = DocumentDetectionResult.noDocument()

    suspend fun renderPreview(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): String

    suspend fun processPage(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
    ): String

    suspend fun processForOcr(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        preferReceiptMode: Boolean,
    ): String
}
