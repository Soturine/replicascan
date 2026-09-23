package com.soturine.replicascan.core.common.model

import java.util.Locale

/**
 * Picks one recognizer deterministically. The app language is only the starting guess: the user
 * can pick another script, and the UI language never implies OCR support (Arabic UI is supported,
 * Arabic OCR is not offered by ML Kit Text Recognition v2).
 */
object OcrScriptPolicy {
    fun defaultFor(locale: Locale): OcrScript = when (locale.language) {
        "ja" -> OcrScript.JAPANESE
        "ko" -> OcrScript.KOREAN
        "hi", "mr", "ne" -> OcrScript.DEVANAGARI
        else -> OcrScript.LATIN
    }
}
