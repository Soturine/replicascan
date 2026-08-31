package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.DocumentQuad
import com.soturine.replicascan.core.common.model.ImagePipelineSpec
import com.soturine.replicascan.core.common.model.PageRenderPurpose
import com.soturine.replicascan.core.common.model.PointValue
import com.soturine.replicascan.core.common.model.ScanPage
import com.soturine.replicascan.core.common.model.coerceNormalized
import com.soturine.replicascan.core.common.model.requiresDerivedImage
import com.soturine.replicascan.core.common.model.toPixelQuad
import com.soturine.replicascan.core.common.model.withInvalidatedDerivedImage
import org.junit.Test

class ImagePipelineSpecTest {
    @Test
    fun `deve normalizar rotacao para um ciclo positivo`() {
        assertThat(ImagePipelineSpec.normalizeRotation(450)).isEqualTo(90)
        assertThat(ImagePipelineSpec.normalizeRotation(-90)).isEqualTo(270)
        assertThat(ImagePipelineSpec.normalizeRotation(720)).isEqualTo(0)
    }

    @Test
    fun `deve montar chave de pipeline com estado visual canonico`() {
        val page = page(
            quad = DocumentQuad(
                topLeft = PointValue(0.1f, 0.2f),
                topRight = PointValue(0.9f, 0.2f),
                bottomRight = PointValue(0.9f, 0.8f),
                bottomLeft = PointValue(0.1f, 0.8f),
            ),
            rotationDegrees = 450,
        )

        val key = ImagePipelineSpec.buildKey(
            page = page,
            purpose = PageRenderPurpose.PREVIEW,
            filterType = DocumentFilterType.DOCUMENT_GRAY,
            maxDimension = 1600,
        ).toString()

        assertThat(key).contains("v=${ImagePipelineSpec.VERSION}")
        assertThat(key).contains("purpose=preview")
        assertThat(key).contains("filter=document_gray")
        assertThat(key).contains("rotation=90")
        assertThat(key).contains("max=1600")
        assertThat(key).contains("0.1000,0.2000")
    }

    @Test
    fun `deve diferenciar preview ocr e exportacao na chave`() {
        val page = page()

        val previewKey = ImagePipelineSpec.buildKey(page, PageRenderPurpose.PREVIEW, maxDimension = 1400)
        val ocrKey = ImagePipelineSpec.buildKey(page, PageRenderPurpose.OCR, maxDimension = 2600)
        val exportKey = ImagePipelineSpec.buildKey(page, PageRenderPurpose.EXPORT)

        assertThat(previewKey).isNotEqualTo(ocrKey)
        assertThat(exportKey).isNotEqualTo(previewKey)
        assertThat(exportKey.toString()).contains("max=full")
    }

    @Test
    fun `deve limitar pontos normalizados e converter para pixels`() {
        val normalized = DocumentQuad(
            topLeft = PointValue(-0.2f, 0.1f),
            topRight = PointValue(1.3f, -0.1f),
            bottomRight = PointValue(1.1f, 1.4f),
            bottomLeft = PointValue(0.2f, 1.2f),
        ).coerceNormalized()

        assertThat(normalized.topLeft).isEqualTo(PointValue(0f, 0.1f))
        assertThat(normalized.topRight).isEqualTo(PointValue(1f, 0f))
        assertThat(normalized.bottomRight).isEqualTo(PointValue(1f, 1f))

        val pixelQuad = normalized.toPixelQuad(width = 1000, height = 600)
        assertThat(pixelQuad.topRight).isEqualTo(PointValue(1000f, 0f))
        assertThat(pixelQuad.bottomLeft).isEqualTo(PointValue(200f, 600f))
    }

    @Test
    fun `deve escolher fonte derivada somente quando ha transformacao local`() {
        assertThat(page().requiresDerivedImage()).isFalse()
        assertThat(page(processedUri = "file:///tmp/processed.jpg").requiresDerivedImage()).isTrue()
        assertThat(page(rotationDegrees = 90).requiresDerivedImage()).isTrue()
        assertThat(page(filterType = DocumentFilterType.COLOR_ENHANCED).requiresDerivedImage()).isTrue()
        assertThat(page(quad = fullQuad()).requiresDerivedImage()).isTrue()
    }

    @Test
    fun `deve invalidar imagem processada e ocr quando o estado visual muda`() {
        val invalidated = page(
            processedUri = "file:///tmp/old.jpg",
            ocrText = "texto antigo",
        ).withInvalidatedDerivedImage()

        assertThat(invalidated.processedUri).isNull()
        assertThat(invalidated.ocrText).isNull()
    }

    private fun page(
        processedUri: String? = null,
        filterType: DocumentFilterType = DocumentFilterType.ORIGINAL_CORRECTED,
        rotationDegrees: Int = 0,
        quad: DocumentQuad? = null,
        ocrText: String? = null,
    ): ScanPage =
        ScanPage(
            id = "page-1",
            scanId = "scan-1",
            index = 0,
            sourceUri = "/stable/source.jpg",
            processedUri = processedUri,
            filterType = filterType,
            rotationDegrees = rotationDegrees,
            quad = quad,
            ocrText = ocrText,
        )

    private fun fullQuad(): DocumentQuad =
        DocumentQuad(
            topLeft = PointValue(0f, 0f),
            topRight = PointValue(1f, 0f),
            bottomRight = PointValue(1f, 1f),
            bottomLeft = PointValue(0f, 1f),
        )
}
