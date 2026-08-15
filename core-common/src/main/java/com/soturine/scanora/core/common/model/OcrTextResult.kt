package com.soturine.scanora.core.common.model

data class OcrTextBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int
        get() = (right - left).coerceAtLeast(0)

    val height: Int
        get() = (bottom - top).coerceAtLeast(0)

    val area: Int
        get() = width * height

    fun union(other: OcrTextBounds): OcrTextBounds =
        OcrTextBounds(
            left = minOf(left, other.left),
            top = minOf(top, other.top),
            right = maxOf(right, other.right),
            bottom = maxOf(bottom, other.bottom),
        )
}

data class OcrTextLine(
    val text: String,
    val bounds: OcrTextBounds? = null,
    val confidence: Float? = null,
    val elements: List<OcrTextElement> = emptyList(),
)

data class OcrTextElement(
    val text: String,
    val bounds: OcrTextBounds? = null,
    val confidence: Float? = null,
)

enum class OcrScript { AUTOMATIC, LATIN, DEVANAGARI, JAPANESE, KOREAN }

enum class OcrModelReadiness { READY, DOWNLOAD_PENDING, UNAVAILABLE }

enum class OcrFailureReason { IMAGE_UNREADABLE, MODEL_NOT_READY, RECOGNITION_FAILED }

data class OcrArtifactMetadata(
    val script: OcrScript,
    val engine: String,
    val engineVersion: String,
    val pipelineVersion: String,
    val sourceFingerprint: String,
    val createdAtEpochMillis: Long,
)

data class OcrTextBlock(
    val lines: List<OcrTextLine>,
    val bounds: OcrTextBounds? = lines.mapNotNull { it.bounds }.unionOrNull(),
) {
    val text: String
        get() = OcrTextPostProcessor.joinLines(lines.map { it.text })
}

data class OcrTextParagraph(
    val lines: List<OcrTextLine>,
    val bounds: OcrTextBounds? = lines.mapNotNull { it.bounds }.unionOrNull(),
) {
    val text: String
        get() = OcrTextPostProcessor.joinLines(lines.map { it.text })

    val wordCount: Int
        get() = text.split(Regex("\\s+")).count { token -> token.any(Char::isLetterOrDigit) }
}

enum class OcrTextQuality {
    EMPTY,
    WEAK,
    PARTIAL,
    GOOD,
}

data class OcrProcessedText(
    val paragraphs: List<OcrTextParagraph>,
    val consolidatedText: String,
    val quality: OcrTextQuality,
    val discardedNoiseCount: Int,
)

private fun Iterable<OcrTextBounds>.unionOrNull(): OcrTextBounds? =
    fold<OcrTextBounds, OcrTextBounds?>(null) { accumulated, bounds ->
        accumulated?.union(bounds) ?: bounds
    }

data class OcrTextResult(
    val blocks: List<OcrTextBlock>,
    val fallbackText: String = "",
    val processedText: OcrProcessedText = OcrTextPostProcessor.process(blocks, fallbackText),
    val metadata: OcrArtifactMetadata? = null,
) {
    val fullText: String
        get() = processedText.consolidatedText.ifBlank { fallbackText.trim() }

    val paragraphs: List<OcrTextParagraph>
        get() = processedText.paragraphs

    val quality: OcrTextQuality
        get() = processedText.quality

    companion object {
        val Empty = OcrTextResult(blocks = emptyList())

        fun fromPlainText(text: String): OcrTextResult {
            val blocks = text
                .split(Regex("\\n{2,}"))
                .mapNotNull { rawBlock ->
                    val lines = rawBlock
                        .lines()
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .map(::OcrTextLine)
                    lines.takeIf { it.isNotEmpty() }?.let(::OcrTextBlock)
                }
            return OcrTextResult(
                blocks = blocks,
                fallbackText = text,
            )
        }
    }
}
