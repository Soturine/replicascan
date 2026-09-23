package com.soturine.replicascan.core.common.repository

import android.graphics.Bitmap
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad

/** Derives page images from the canonical private source. Every result is regenerable cache. */
interface DocumentProcessingRepository {
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

    /** Full-quality render kept in memory so exports never pass through a lossy intermediate file. */
    suspend fun renderBitmap(
        sourceUri: String,
        filterType: DocumentFilterType,
        quad: DocumentQuad?,
        rotationDegrees: Int,
        maxDimension: Int,
    ): Bitmap

    /** Geometry-only render (crop and rotation) handed to text recognition. */
    suspend fun processForOcr(
        sourceUri: String,
        quad: DocumentQuad?,
        rotationDegrees: Int,
    ): String
}
