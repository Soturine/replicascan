package com.soturine.replicascan.core.data.ocr

import android.content.Context
import android.graphics.Rect
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.soturine.replicascan.core.common.image.CanonicalImageDecoder
import com.soturine.replicascan.core.common.image.ImagePurpose
import com.soturine.replicascan.core.common.model.OcrArtifactMetadata
import com.soturine.replicascan.core.common.model.OcrFailureReason
import com.soturine.replicascan.core.common.model.OcrModelReadiness
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrTextBlock
import com.soturine.replicascan.core.common.model.OcrTextBounds
import com.soturine.replicascan.core.common.model.OcrTextElement
import com.soturine.replicascan.core.common.model.OcrTextLine
import com.soturine.replicascan.core.common.model.OcrTextResult
import com.soturine.replicascan.core.common.repository.OcrRecognitionException
import com.soturine.replicascan.core.common.repository.OcrRepository
import com.soturine.replicascan.core.common.repository.OcrRequest
import com.soturine.replicascan.core.data.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ML Kit Text Recognition v2 through Google Play services. One recognizer runs per request. */
class DefaultOcrRepository(context: Context) : OcrRepository, AutoCloseable {
    private val appContext = context.applicationContext
    private val imageDecoder = CanonicalImageDecoder(appContext)
    private val recognizers = ConcurrentHashMap<OcrScript, TextRecognizer>()

    override suspend fun recognize(request: OcrRequest): OcrTextResult = withContext(Dispatchers.IO) {
        val decoded = imageDecoder.decode(request.imageUri, ImagePurpose.OCR)
            ?: throw OcrRecognitionException(OcrFailureReason.IMAGE_UNREADABLE)
        try {
            val recognized = recognizer(request.script)
                .process(InputImage.fromBitmap(decoded.bitmap, 0))
                .awaitResult()
            toResult(recognized, request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: MlKitException) {
            val reason = if (exception.errorCode == MlKitException.UNAVAILABLE) {
                OcrFailureReason.MODEL_NOT_READY
            } else {
                OcrFailureReason.RECOGNITION_FAILED
            }
            throw OcrRecognitionException(reason, exception)
        } catch (exception: Exception) {
            throw OcrRecognitionException(OcrFailureReason.RECOGNITION_FAILED, exception)
        } finally {
            decoded.bitmap.recycle()
        }
    }

    override suspend fun modelReadiness(script: OcrScript): OcrModelReadiness = withContext(Dispatchers.IO) {
        val installer = ModuleInstall.getClient(appContext)
        val api = recognizer(script)
        try {
            if (installer.areModulesAvailable(api).awaitResult().areModulesAvailable()) {
                OcrModelReadiness.READY
            } else {
                // Fire-and-forget: Play services downloads in the background; a later run becomes READY.
                installer.installModules(ModuleInstallRequest.newBuilder().addApi(api).build())
                OcrModelReadiness.DOWNLOAD_PENDING
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ApiException) {
            if (exception.statusCode == CommonStatusCodes.API_NOT_CONNECTED) OcrModelReadiness.UNAVAILABLE else OcrModelReadiness.ERROR
        } catch (_: Exception) {
            OcrModelReadiness.ERROR
        }
    }

    override fun close() {
        recognizers.values.forEach(TextRecognizer::close)
        recognizers.clear()
    }

    private fun recognizer(script: OcrScript): TextRecognizer = recognizers.getOrPut(script) {
        when (script) {
            OcrScript.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            OcrScript.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            OcrScript.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrScript.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        }
    }

    private fun toResult(result: Text, request: OcrRequest): OcrTextResult {
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
            lines.takeIf(List<OcrTextLine>::isNotEmpty)?.let { OcrTextBlock(it, block.boundingBox?.toOcrTextBounds()) }
        }
        return OcrTextResult(
            blocks = blocks,
            fallbackText = result.text.trim(),
            metadata = OcrArtifactMetadata(
                script = request.script,
                engine = ENGINE,
                engineVersion = if (request.script == OcrScript.LATIN) {
                    BuildConfig.OCR_LATIN_ENGINE_VERSION
                } else {
                    BuildConfig.OCR_SCRIPT_ENGINE_VERSION
                },
                pipelineVersion = request.pipelineVersion,
                sourceFingerprint = request.sourceFingerprint,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun Rect.toOcrTextBounds() = OcrTextBounds(left, top, right, bottom)

    private fun List<Float>.averageOrNull(): Float? = if (isEmpty()) null else average().toFloat()

    private companion object {
        const val ENGINE = "ml-kit-text-recognition-v2"
    }
}
