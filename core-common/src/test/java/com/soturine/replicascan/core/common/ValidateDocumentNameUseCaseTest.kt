package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.result.NameValidationError
import com.soturine.replicascan.core.common.usecase.ValidateDocumentNameUseCase
import org.junit.Test

class ValidateDocumentNameUseCaseTest {
    private val useCase = ValidateDocumentNameUseCase()

    @Test
    fun `deve rejeitar nome em branco`() {
        val result = useCase("   ")

        assertThat(result.isValid).isFalse()
        assertThat(result.error).isEqualTo(NameValidationError.BLANK)
    }

    @Test
    fun `deve aceitar nomes curtos reais`() {
        assertThat(useCase("RG").isValid).isTrue()
    }

    @Test
    fun `deve rejeitar nome longo demais`() {
        val result = useCase("a".repeat(ValidateDocumentNameUseCase.MAX_LENGTH + 1))

        assertThat(result.isValid).isFalse()
        assertThat(result.error).isEqualTo(NameValidationError.TOO_LONG)
    }

    @Test
    fun `deve sanitizar caracteres proibidos`() {
        val result = useCase("Contrato: João/2026")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("Contrato João2026")
    }
}
