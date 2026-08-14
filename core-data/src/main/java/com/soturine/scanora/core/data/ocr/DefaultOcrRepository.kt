package com.soturine.scanora.core.data.ocr

import android.content.Context
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.soturine.scanora.core.common.image.CanonicalImageDecoder
import com.soturine.scanora.core.common.image.ImagePurpose
import com.soturine.scanora.core.common.model.OcrArtifactMetadata
import com.soturine.scanora.core.common.model.OcrFailureReason
import com.soturine.scanora.core.common.model.OcrModelReadiness
import com.soturine.scanora.core.common.model.OcrScript
import com.soturine.scanora.core.common.model.OcrTextBlock
import com.soturine.scanora.core.common.model.OcrTextBounds
import com.soturine.scanora.core.common.model.OcrTextElement
import com.soturine.scanora.core.common.model.OcrTextLine
import com.soturine.scanora.core.common.model.OcrTextResult
import com.soturine.scanora.core.common.repository.OcrRepository
import com.soturine.scanora.core.common.repository.OcrRequest
import com.soturine.scanora.core.common.repository.OcrRecognitionException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultOcrRepository(context: Context) : OcrRepository, AutoCloseable {
    private val imageDecoder = CanonicalImageDecoder(context.applicationContext)
    private val readiness = ConcurrentHashMap<OcrScript, OcrModelReadiness>().apply {
        OcrScript.entries.forEach { put(it, OcrModelReadiness.DOWNLOAD_PENDING) }
    }
    private val recognizerClients: Map<OcrScript, TextRecognizer> by lazy {
        mapOf(
            OcrScript.LATIN to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            OcrScript.DEVANAGARI to TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
            OcrScript.JAPANESE to TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            OcrScript.KOREAN to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        )
    }

    override suspend fun recognize(request: OcrRequest): OcrTextResult = withContext(Dispatchers.IO) {
        val decoded = imageDecoder.decode(request.imageUri, ImagePurpose.OCR)
            ?: throw OcrRecognitionException(OcrFailureReason.IMAGE_UNREADABLE)
        try {
            val recognized = recognizerClients.getValue(request.script)
                .process(InputImage.fromBitmap(decoded.bitmap, 0))
                .awaitResult()
            readiness[request.script] = OcrModelReadiness.READY
            formatRecognizedText(recognized, request)
        } catch (exception: MlKitException) {
            val reason = if (exception.errorCode == 14) {
                readiness[request.script] = OcrModelReadiness.DOWNLOAD_PENDING
                OcrFailureReason.MODEL_NOT_READY
            } else {
                OcrFailureReason.RECOGNITION_FAILED
            }
            throw OcrRecognitionException(reason, exception)
        }
    }

    override suspend fun modelReadiness(script: OcrScript): OcrModelReadiness =
        readiness[script] ?: OcrModelReadiness.UNAVAILABLE

    override fun close() {
        recognizerClients.values.forEach(TextRecognizer::close)
    }

    private fun formatRecognizedText(result: Text, request: OcrRequest): OcrTextResult {
        val blocks = result.textBlocks.mapNotNull { block ->
            val lines = block.lines.mapNotNull { line ->
                line.text.trim().takeIf(String::isNotBlank)?.let { text ->
                    val elements = line.elements.mapNotNull { element ->
                        element.text.trim().takeIf(String::isNotBlank)?.let { value ->
                            OcrTextElement(value, element.boundingBox?.toOcrTextBounds(), element.confidence)
                        }
                    }
                    OcrTextLine(
                        text = text,
                        bounds = line.boundingBox?.toOcrTextBounds(),
                        confidence = elements.mapNotNull(OcrTextElement::confidence).averageOrNull(),
                        elements = elements,
                    )
                }
            }
            lines.takeIf(List<OcrTextLine>::isNotEmpty)?.let {
                OcrTextBlock(it, block.boundingBox?.toOcrTextBounds())
            }
        }
        return OcrTextResult(
            blocks = blocks,
            fallbackText = result.text.trim(),
            metadata = OcrArtifactMetadata(
                script = request.script,
                engine = "ml-kit-text-recognition-v2",
                engineVersion = if (request.script == OcrScript.LATIN) "19.0.1" else "16.0.1",
                pipelineVersion = request.pipelineVersion,
                sourceFingerprint = request.sourceFingerprint,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun Rect.toOcrTextBounds() = OcrTextBounds(left, top, right, bottom)

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else average().toFloat()
}
