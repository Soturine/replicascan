package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.ScanDocument
import com.soturine.replicascan.core.common.model.ScanMode
import com.soturine.replicascan.core.common.usecase.SearchScansUseCase
import org.junit.Test

class SearchScansUseCaseTest {
    private val useCase = SearchScansUseCase()

    @Test
    fun `deve buscar por titulo e tags`() {
        val scans = listOf(
            ScanDocument(
                id = "1",
                title = "Contrato locacao",
                mode = ScanMode.DOCUMENT,
                tags = listOf("juridico"),
                isFavorite = false,
                createdAt = 0,
                updatedAt = 0,
                pages = emptyList(),
                isDraft = false,
            ),
            ScanDocument(
                id = "2",
                title = "Caderno de algoritmos",
                mode = ScanMode.NOTEBOOK,
                tags = listOf("faculdade"),
                isFavorite = false,
                createdAt = 0,
                updatedAt = 0,
                pages = emptyList(),
                isDraft = false,
            ),
        )

        val result = useCase(scans, "faculdade")

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("2")
    }
}
