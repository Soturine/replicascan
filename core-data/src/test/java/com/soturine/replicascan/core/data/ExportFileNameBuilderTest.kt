package com.soturine.replicascan.core.data

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.ExportFormat
import com.soturine.replicascan.core.data.export.ExportFileNameBuilder
import org.junit.Test

class ExportFileNameBuilderTest {
    private val builder = ExportFileNameBuilder(
        currentTimeMillis = { 1_776_774_645_000L },
    )

    @Test
    fun `deve gerar nome base sanitizado`() {
        val fileName = builder.buildBaseName("Contrato João / Abril", ExportFormat.PDF)

        assertThat(fileName).isEqualTo("contrato-joao-abril-20260421-093045.pdf")
    }

    @Test
    fun `deve gerar nome de pagina`() {
        val fileName = builder.buildPageName("Apostila Algoritmos", 1, ExportFormat.JPG)

        assertThat(fileName).isEqualTo("apostila-algoritmos-20260421-093045-p02.jpg")
    }
}
