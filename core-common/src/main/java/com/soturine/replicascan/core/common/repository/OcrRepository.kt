package com.soturine.replicascan.core.common.repository

import com.soturine.replicascan.core.common.model.OcrFailureReason
import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrTextResult

data class OcrRequest(
    val imageUri: String,
    val script: OcrScript,
    val sourceFingerprint: String = imageUri,
    val pipelineVersion: String = PIPELINE_VERSION,
) {
    companion object {
        /** v4: one recognizer per run on the geometry-only page render (no binarization). */
        const val PIPELINE_VERSION = "ocr-v4"
    }
}

class OcrRecognitionException(
    val reason: OcrFailureReason,
    cause: Throwable? = null,
) : IllegalStateException(reason.name, cause)

interface OcrRepository {
    suspend fun recognize(request: OcrRequest): OcrTextResult

    /** Asks Google Play services whether the script's model is installed, requesting it if not. */
    suspend fun modelReadiness(script: OcrScript): OcrModelReadiness
}
