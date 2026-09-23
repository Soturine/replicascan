package com.soturine.replicascan.core.common.result

enum class NameValidationError {
    BLANK,
    TOO_LONG,
}

data class ValidationResult(
    val isValid: Boolean,
    val sanitizedValue: String,
    val error: NameValidationError? = null,
)
