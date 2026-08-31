package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.usecase.ValidateDocumentNameUseCase
import org.junit.Test

class ValidateDocumentNameUseCaseTest {
    private val useCase = ValidateDocumentNameUseCase()

    @Test
    fun `deve rejeitar nome em branco`() {
        val result = useCase("   ")

        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("Informe um nome para o documento.")
    }

    @Test
    fun `deve sanitizar caracteres proibidos`() {
        val result = useCase("Contrato: João/2026")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("Contrato João2026")
    }
}
