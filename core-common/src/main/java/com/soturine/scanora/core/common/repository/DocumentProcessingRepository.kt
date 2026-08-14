package com.soturine.scanora.core.common.repository

import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.DocumentDetectionResult
import com.soturine.scanora.core.common.model.DocumentProfile
import com.soturine.scanora.core.common.model.DocumentQuad

interface DocumentProcessingRepository {
    suspend fun detectDocument(
        imageUri: String,
        profile: DocumentProfile = DocumentProfile.GENERAL,
    ): DocumentDetectionResult

    suspend fun estimateDocumentQuad(imageUri: String): DocumentQuad =
        detectDocument(imageUri).quadOrFullPage()

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
