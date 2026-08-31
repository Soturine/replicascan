package com.soturine.replicascan.core.common.usecase

import com.soturine.replicascan.core.common.result.ValidationResult

class ValidateDocumentNameUseCase {
    operator fun invoke(rawValue: String): ValidationResult {
        val sanitized = rawValue
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")

        return when {
            sanitized.isBlank() -> ValidationResult(
                isValid = false,
                sanitizedValue = sanitized,
                errorMessage = "Informe um nome para o documento.",
            )

            sanitized.length < 3 -> ValidationResult(
                isValid = false,
                sanitizedValue = sanitized,
                errorMessage = "Use pelo menos 3 caracteres.",
            )

            sanitized.length > 80 -> ValidationResult(
                isValid = false,
                sanitizedValue = sanitized.take(80),
                errorMessage = "Use no máximo 80 caracteres.",
            )

            else -> ValidationResult(
                isValid = true,
                sanitizedValue = sanitized,
            )
        }
    }
}
