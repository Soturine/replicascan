package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.OcrTextBlock
import com.soturine.replicascan.core.common.model.OcrTextBounds
import com.soturine.replicascan.core.common.model.OcrTextLine
import com.soturine.replicascan.core.common.model.OcrTextQuality
import com.soturine.replicascan.core.common.model.OcrTextResult
import org.junit.Test

class OcrTextResultTest {
    @Test
    fun `deve montar blocos a partir de texto salvo`() {
        val result = OcrTextResult.fromPlainText("Linha 1\nLinha 2\n\nOutro bloco")

        assertThat(result.blocks).hasSize(2)
        assertThat(result.blocks.first().lines.map { it.text }).containsExactly("Linha 1", "Linha 2")
        assertThat(result.paragraphs).hasSize(2)
        assertThat(result.fullText).isEqualTo("Linha 1 Linha 2\n\nOutro bloco")
    }

    @Test
    fun `deve ordenar linhas por posicao visual`() {
        val result = OcrTextResult(
            blocks = listOf(
                block(
                    line("Linha de baixo", left = 20, top = 38, right = 220, bottom = 60),
                    line("continuação", left = 180, top = 12, right = 280, bottom = 34),
                    line("Linha de cima", left = 20, top = 10, right = 210, bottom = 32),
                ),
            ),
        )

        assertThat(result.fullText).isEqualTo("Linha de cima continuação Linha de baixo")
    }

    @Test
    fun `deve agrupar linhas proximas e separar paragrafos por espaco vertical`() {
        val result = OcrTextResult(
            blocks = listOf(
                block(
                    line("Primeira linha", left = 12, top = 10, right = 180, bottom = 30),
                    line("continua aqui", left = 12, top = 36, right = 190, bottom = 56),
                    line("Novo paragrafo", left = 12, top = 128, right = 210, bottom = 148),
                    line("com contexto", left = 12, top = 154, right = 190, bottom = 174),
                ),
            ),
        )

        assertThat(result.paragraphs.map { it.text })
            .containsExactly(
                "Primeira linha continua aqui",
                "Novo paragrafo com contexto",
            )
            .inOrder()
        assertThat(result.fullText)
            .isEqualTo("Primeira linha continua aqui\n\nNovo paragrafo com contexto")
    }

    @Test
    fun `deve descartar ruido pequeno e manter texto util`() {
        val result = OcrTextResult(
            blocks = listOf(
                block(
                    line(".", left = 4, top = 4, right = 6, bottom = 6),
                    line("x", left = 9, top = 5, right = 11, bottom = 8),
                    line("Total do recibo", left = 20, top = 20, right = 170, bottom = 42),
                    line("R$ 42,00", left = 20, top = 48, right = 130, bottom = 70),
                ),
            ),
        )

        assertThat(result.fullText).isEqualTo("Total do recibo R$ 42,00")
        assertThat(result.processedText.discardedNoiseCount).isEqualTo(2)
    }

    @Test
    fun `deve consolidar fragmentos pequenos em trecho util`() {
        val result = OcrTextResult(
            blocks = listOf(
                block(
                    line("Total", left = 12, top = 10, right = 70, bottom = 30),
                    line("Pagamento realizado com cartao", left = 12, top = 72, right = 260, bottom = 92),
                    line("em 25 de abril", left = 12, top = 98, right = 180, bottom = 118),
                ),
            ),
        )

        assertThat(result.paragraphs).hasSize(1)
        assertThat(result.fullText).isEqualTo("Total Pagamento realizado com cartao em 25 de abril")
        assertThat(result.quality).isEqualTo(OcrTextQuality.PARTIAL)
    }

    private fun block(vararg lines: OcrTextLine): OcrTextBlock = OcrTextBlock(lines.toList())

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): OcrTextLine = OcrTextLine(
        text = text,
        bounds = OcrTextBounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        ),
    )
}
