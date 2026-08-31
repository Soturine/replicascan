package com.soturine.replicascan.core.common.model

import kotlin.math.abs
import kotlin.math.max

object OcrTextPostProcessor {
    private val whitespaceRegex = Regex("\\s+")
    private val spaceBeforePunctuationRegex = Regex("\\s+([,.;:!?])")
    private val openingSpaceRegex = Regex("([({\\[])\\s+")

    fun process(
        blocks: List<OcrTextBlock>,
        fallbackText: String = "",
    ): OcrProcessedText {
        val candidates = blocks.flatMapIndexed { blockIndex, block ->
            block.lines.mapIndexedNotNull { lineIndex, line ->
                val cleaned = cleanLine(line.text)
                if (cleaned.isBlank() || cleaned.none(Char::isLetterOrDigit)) {
                    null
                } else {
                    CandidateLine(
                        text = cleaned,
                        bounds = line.bounds ?: block.bounds,
                        blockIndex = blockIndex,
                        lineIndex = lineIndex,
                    )
                }
            }
        }
        val rawLineCount = blocks.sumOf { it.lines.size }
        val medianHeight = medianLineHeight(candidates)
        val usefulCandidates = candidates.filterNot { candidate -> candidate.isTinyNoise(medianHeight) }
        val discardedNoiseCount = rawLineCount - usefulCandidates.size

        if (usefulCandidates.isEmpty()) {
            return fromPlainText(fallbackText, discardedNoiseCount)
        }

        val orderedLines = orderLines(usefulCandidates, medianHeight)
        val groupedParagraphs = groupIntoParagraphs(orderedLines, medianHeight)
        val normalizedParagraphs = if (usefulCandidates.any { it.bounds != null }) {
            mergeTinyParagraphs(groupedParagraphs)
        } else {
            groupedParagraphs
        }
        val paragraphs = normalizedParagraphs.map { paragraphLines ->
            OcrTextParagraph(
                lines = paragraphLines.map { candidate ->
                    OcrTextLine(
                        text = candidate.text,
                        bounds = candidate.bounds,
                    )
                },
            )
        }.filter { paragraph -> paragraph.text.isNotBlank() }

        val consolidatedText = paragraphs
            .joinToString(separator = "\n\n") { paragraph -> paragraph.text }
            .trim()

        return OcrProcessedText(
            paragraphs = paragraphs,
            consolidatedText = consolidatedText,
            quality = assessQuality(consolidatedText, paragraphs),
            discardedNoiseCount = discardedNoiseCount,
        )
    }

    fun joinLines(lines: List<String>): String {
        val cleanedLines = lines
            .map(::cleanLine)
            .filter(String::isNotBlank)
        if (cleanedLines.isEmpty()) return ""

        val joined = cleanedLines.fold("") { current, line ->
            when {
                current.isBlank() -> line
                current.endsWith("-") -> current.dropLast(1) + line
                else -> "$current $line"
            }
        }
        return joined
            .replace(spaceBeforePunctuationRegex, "\$1")
            .replace(openingSpaceRegex, "\$1")
            .trim()
    }

    private fun fromPlainText(
        text: String,
        discardedNoiseCount: Int,
    ): OcrProcessedText {
        val paragraphs = text
            .split(Regex("\\n{2,}"))
            .mapNotNull { rawParagraph ->
                val lines = rawParagraph
                    .lines()
                    .map(::cleanLine)
                    .filter { line -> line.isNotBlank() && line.any(Char::isLetterOrDigit) }
                    .map(::OcrTextLine)
                lines.takeIf { it.isNotEmpty() }?.let(::OcrTextParagraph)
            }
        val consolidatedText = paragraphs
            .joinToString(separator = "\n\n") { paragraph -> paragraph.text }
            .trim()
            .ifBlank { cleanLine(text) }

        return OcrProcessedText(
            paragraphs = paragraphs,
            consolidatedText = consolidatedText,
            quality = assessQuality(consolidatedText, paragraphs),
            discardedNoiseCount = discardedNoiseCount,
        )
    }

    private fun orderLines(
        candidates: List<CandidateLine>,
        medianHeight: Float,
    ): List<CandidateLine> {
        if (candidates.none { it.bounds != null }) {
            return candidates.sortedWith(compareBy<CandidateLine> { it.blockIndex }.thenBy { it.lineIndex })
        }

        val rowTolerance = max(8f, medianHeight * 0.65f)
        val rows = mutableListOf<MutableList<CandidateLine>>()
        candidates
            .sortedWith(
                compareBy<CandidateLine> { it.bounds?.top ?: Int.MAX_VALUE }
                    .thenBy { it.bounds?.left ?: Int.MAX_VALUE }
                    .thenBy { it.blockIndex }
                    .thenBy { it.lineIndex },
            )
            .forEach { candidate ->
                val row = rows.lastOrNull()
                val rowTop = row?.mapNotNull { it.bounds?.top }?.average()?.toFloat()
                if (
                    row != null &&
                    rowTop != null &&
                    candidate.bounds != null &&
                    abs(candidate.bounds.top - rowTop) <= rowTolerance
                ) {
                    row += candidate
                } else {
                    rows += mutableListOf(candidate)
                }
            }

        return rows.flatMap { row ->
            row.sortedWith(
                compareBy<CandidateLine> { it.bounds?.left ?: Int.MAX_VALUE }
                    .thenBy { it.blockIndex }
                    .thenBy { it.lineIndex },
            )
        }
    }

    private fun groupIntoParagraphs(
        orderedLines: List<CandidateLine>,
        medianHeight: Float,
    ): List<List<CandidateLine>> {
        if (orderedLines.isEmpty()) return emptyList()

        val paragraphGap = max(18f, medianHeight * 1.45f)
        val paragraphs = mutableListOf<MutableList<CandidateLine>>()
        orderedLines.forEach { line ->
            val currentParagraph = paragraphs.lastOrNull()
            val previousLine = currentParagraph?.lastOrNull()
            val shouldStartParagraph = when {
                currentParagraph == null || previousLine == null -> true
                line.bounds == null || previousLine.bounds == null -> line.blockIndex != previousLine.blockIndex
                else -> {
                    val verticalGap = line.bounds.top - previousLine.bounds.bottom
                    verticalGap > paragraphGap
                }
            }

            if (shouldStartParagraph) {
                paragraphs += mutableListOf(line)
            } else {
                currentParagraph?.add(line)
            }
        }
        return paragraphs
    }

    private fun mergeTinyParagraphs(paragraphs: List<List<CandidateLine>>): List<List<CandidateLine>> {
        if (paragraphs.size <= 1) return paragraphs

        val merged = mutableListOf<MutableList<CandidateLine>>()
        var index = 0
        while (index < paragraphs.size) {
            val paragraph = paragraphs[index]
            val isTiny = paragraph.isTinyParagraph()
            when {
                isTiny && index + 1 < paragraphs.size -> {
                    merged += (paragraph + paragraphs[index + 1]).toMutableList()
                    index += 2
                }
                isTiny && merged.isNotEmpty() -> {
                    merged.last() += paragraph
                    index += 1
                }
                else -> {
                    merged += paragraph.toMutableList()
                    index += 1
                }
            }
        }
        return merged
    }

    private fun List<CandidateLine>.isTinyParagraph(): Boolean {
        if (size > 1) return false
        val text = joinLines(map { it.text })
        val words = wordCount(text)
        return words <= 2 || text.length < 18
    }

    private fun CandidateLine.isTinyNoise(medianHeight: Float): Boolean {
        val alphaNumericCount = text.count(Char::isLetterOrDigit)
        val bounds = bounds ?: return false
        val tinyHeight = bounds.height < max(5f, medianHeight * 0.45f)
        val tinyWidth = bounds.width < max(5f, medianHeight * 0.45f)
        return alphaNumericCount <= 1 && (tinyHeight || tinyWidth || bounds.area < 32)
    }

    private fun medianLineHeight(candidates: List<CandidateLine>): Float {
        val heights = candidates
            .mapNotNull { candidate -> candidate.bounds?.height?.takeIf { it > 0 } }
            .sorted()
        if (heights.isEmpty()) return 18f
        val middle = heights.size / 2
        return if (heights.size % 2 == 0) {
            ((heights[middle - 1] + heights[middle]) / 2f)
        } else {
            heights[middle].toFloat()
        }.coerceAtLeast(8f)
    }

    private fun assessQuality(
        consolidatedText: String,
        paragraphs: List<OcrTextParagraph>,
    ): OcrTextQuality {
        if (consolidatedText.isBlank()) return OcrTextQuality.EMPTY
        val words = wordCount(consolidatedText)
        val mostlyTiny = paragraphs.isNotEmpty() && paragraphs.count { it.wordCount <= 2 } > paragraphs.size / 2
        return when {
            words < 5 || consolidatedText.length < 32 -> OcrTextQuality.WEAK
            words < 12 || consolidatedText.length < 90 || mostlyTiny -> OcrTextQuality.PARTIAL
            else -> OcrTextQuality.GOOD
        }
    }

    private fun cleanLine(text: String): String =
        text.replace(whitespaceRegex, " ").trim()

    private fun wordCount(text: String): Int =
        text.split(whitespaceRegex).count { token -> token.any(Char::isLetterOrDigit) }

    private data class CandidateLine(
        val text: String,
        val bounds: OcrTextBounds?,
        val blockIndex: Int,
        val lineIndex: Int,
    )
}
