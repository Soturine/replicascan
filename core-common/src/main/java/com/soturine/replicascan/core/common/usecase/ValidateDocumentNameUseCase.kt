package com.soturine.replicascan.core.common.usecase

import com.soturine.replicascan.core.common.result.NameValidationError
import com.soturine.replicascan.core.common.result.ValidationResult

/** Keeps document names safe for export file names; the UI maps errors to localized copy. */
class ValidateDocumentNameUseCase {
    operator fun invoke(rawValue: String): ValidationResult {
        val sanitized = rawValue
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")

        return when {
            sanitized.isBlank() -> ValidationResult(false, sanitized, NameValidationError.BLANK)
            sanitized.length > MAX_LENGTH -> ValidationResult(false, sanitized.take(MAX_LENGTH), NameValidationError.TOO_LONG)
            else -> ValidationResult(true, sanitized)
        }
    }

    companion object {
        const val MAX_LENGTH = 80
    }
}
