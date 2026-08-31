package com.soturine.replicascan.core.data.repository

import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.PointValue
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanMode
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.data.local.ScanWithPages
import com.soturine.replicascan.core.data.local.entity.PageEntity
import com.soturine.replicascan.core.data.local.entity.ScanEntity

internal fun ScanWithPages.asExternalModel(): ScanDocument =
    ScanDocument(
        id = scan.id,
        title = scan.title,
        mode = ScanMode.fromStorageKey(scan.mode),
        tags = TagCodec.decode(scan.tags),
        isFavorite = scan.isFavorite,
        createdAt = scan.createdAt,
        updatedAt = scan.updatedAt,
        pages = pages.sortedBy { it.pageIndex }.map(PageEntity::asExternalModel),
        isDraft = scan.isDraft,
    )

internal fun PageEntity.asExternalModel(): ScanPage =
    ScanPage(
        id = id,
        scanId = scanId,
        index = pageIndex,
        sourceUri = sourceUri,
        processedUri = processedUri,
        filterType = DocumentFilterType.fromStorageKey(filterType),
        rotationDegrees = rotationDegrees,
        quad = quad?.toDocumentQuad(),
        ocrText = ocrText,
    )

internal fun ScanPage.asEntity(): PageEntity =
    PageEntity(
        id = id,
        scanId = scanId,
        pageIndex = index,
        sourceUri = sourceUri,
        processedUri = processedUri,
        filterType = filterType.storageKey,
        rotationDegrees = rotationDegrees,
        quad = quad?.serialize(),
        ocrText = ocrText,
    )

internal fun ScanEntity.withChanges(
    title: String = this.title,
    tags: String = this.tags,
    isFavorite: Boolean = this.isFavorite,
    updatedAt: Long = this.updatedAt,
    isDraft: Boolean = this.isDraft,
): ScanEntity =
    copy(
        title = title,
        tags = tags,
        isFavorite = isFavorite,
        updatedAt = updatedAt,
        isDraft = isDraft,
    )

private fun DocumentQuad.serialize(): String =
    asList().joinToString("|") { point -> "${point.x},${point.y}" }

private fun String.toDocumentQuad(): DocumentQuad {
    val points = split("|").mapNotNull { token ->
        val values = token.split(",")
        if (values.size != 2) return@mapNotNull null
        PointValue(
            x = values[0].toFloatOrNull() ?: return@mapNotNull null,
            y = values[1].toFloatOrNull() ?: return@mapNotNull null,
        )
    }
    return if (points.size == 4) {
        DocumentQuad(
            topLeft = points[0],
            topRight = points[1],
            bottomRight = points[2],
            bottomLeft = points[3],
        )
    } else {
        DocumentQuad(
            topLeft = PointValue(0f, 0f),
            topRight = PointValue(1f, 0f),
            bottomRight = PointValue(1f, 1f),
            bottomLeft = PointValue(0f, 1f),
        )
    }
}

