package com.soturine.replicascan.core.common.model

import java.util.Locale

enum class PageRenderPurpose {
    PREVIEW,
    SAVED_DERIVATIVE,
    OCR,
    EXPORT,
}

data class PagePipelineKey(
    val sourceUri: String,
    val filterType: DocumentFilterType,
    val rotationDegrees: Int,
    val quadKey: String,
    val purpose: PageRenderPurpose,
    val maxDimension: Int?,
    val algorithmVersion: Int,
) {
    override fun toString(): String = buildString {
        append("v=")
        append(algorithmVersion)
        append("|purpose=")
        append(purpose.name.lowercase(Locale.US))
        append("|source=")
        append(sourceUri)
        append("|filter=")
        append(filterType.storageKey)
        append("|rotation=")
        append(rotationDegrees)
        append("|max=")
        append(maxDimension ?: "full")
        append("|quad=")
        append(quadKey)
    }
}

object ImagePipelineSpec {
    const val VERSION = 3

    fun normalizeRotation(rotationDegrees: Int): Int =
        ((rotationDegrees % 360) + 360) % 360

    fun buildKey(
        page: ScanPage,
        purpose: PageRenderPurpose,
        filterType: DocumentFilterType = page.filterType,
        maxDimension: Int? = null,
    ): PagePipelineKey =
        PagePipelineKey(
            sourceUri = page.sourceUri,
            filterType = filterType,
            rotationDegrees = normalizeRotation(page.rotationDegrees),
            quadKey = page.quad?.coerceNormalized()?.cacheKey() ?: "full-page",
            purpose = purpose,
            maxDimension = maxDimension,
            algorithmVersion = VERSION,
        )
}

fun ScanPage.requiresDerivedImage(): Boolean =
    processedUri != null ||
        quad != null ||
        ImagePipelineSpec.normalizeRotation(rotationDegrees) != 0 ||
        filterType != DocumentFilterType.ORIGINAL_CORRECTED

fun ScanPage.withInvalidatedDerivedImage(clearOcr: Boolean = true): ScanPage =
    copy(
        processedUri = null,
        ocrText = if (clearOcr) null else ocrText,
    )

fun DocumentQuad.coerceNormalized(): DocumentQuad =
    DocumentQuad(
        topLeft = topLeft.coerceNormalized(),
        topRight = topRight.coerceNormalized(),
        bottomRight = bottomRight.coerceNormalized(),
        bottomLeft = bottomLeft.coerceNormalized(),
    )

fun DocumentQuad.toPixelQuad(
    width: Int,
    height: Int,
): DocumentQuad {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val normalized = coerceNormalized()
    return DocumentQuad(
        topLeft = normalized.topLeft.toPixelPoint(safeWidth, safeHeight),
        topRight = normalized.topRight.toPixelPoint(safeWidth, safeHeight),
        bottomRight = normalized.bottomRight.toPixelPoint(safeWidth, safeHeight),
        bottomLeft = normalized.bottomLeft.toPixelPoint(safeWidth, safeHeight),
    )
}

private fun PointValue.coerceNormalized(): PointValue =
    PointValue(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
    )

private fun PointValue.toPixelPoint(
    width: Int,
    height: Int,
): PointValue =
    PointValue(
        x = x * width,
        y = y * height,
    )

private fun DocumentQuad.cacheKey(): String = buildString {
    append(topLeft.compact())
    append('|')
    append(topRight.compact())
    append('|')
    append(bottomRight.compact())
    append('|')
    append(bottomLeft.compact())
}

private fun PointValue.compact(): String =
    "${x.formatForCache()},${y.formatForCache()}"

private fun Float.formatForCache(): String = String.format(Locale.US, "%.4f", this)
