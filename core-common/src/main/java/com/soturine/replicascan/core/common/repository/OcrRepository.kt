package com.soturine.replicascan.core.common.repository

import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrTextResult
import com.soturine.replicascan.core.common.model.OcrFailureReason

data class OcrRequest(
    val imageUri: String,
    val script: OcrScript = OcrScript.AUTOMATIC,
    val fallbackHint: OcrScript? = null,
    val sourceFingerprint: String = imageUri,
    val pipelineVersion: String = "ocr-v3",
)

class OcrRecognitionException(
    val reason: OcrFailureReason,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)

interface OcrRepository {
    suspend fun recognize(request: OcrRequest): OcrTextResult

    suspend fun modelReadiness(script: OcrScript): OcrModelReadiness

    suspend fun recognizeText(imageUri: String): OcrTextResult = recognize(OcrRequest(imageUri))
}

