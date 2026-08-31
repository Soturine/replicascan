package com.soturine.replicascan.core.common.model

/** Keeps automatic OCR predictable: one locale-informed attempt plus Latin fallback. */
object AutomaticOcrScriptPlanner {
    fun candidates(
        requested: OcrScript,
        localeHint: OcrScript?,
    ): List<OcrScript> {
        if (requested != OcrScript.AUTOMATIC) return listOf(requested)
        val hint = localeHint?.takeUnless { it == OcrScript.AUTOMATIC }
        return listOfNotNull(hint, OcrScript.LATIN.takeUnless { hint == OcrScript.LATIN })
            .distinct()
            .take(2)
    }
}
