package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.model.OcrScript
import com.soturine.replicascan.core.common.model.OcrScriptPolicy
import java.util.Locale
import org.junit.Test

class OcrScriptPolicyTest {
    @Test
    fun cjkAndDevanagariLocalesStartWithTheirOwnRecognizer() {
        assertThat(OcrScriptPolicy.defaultFor(Locale.JAPANESE)).isEqualTo(OcrScript.JAPANESE)
        assertThat(OcrScriptPolicy.defaultFor(Locale.KOREAN)).isEqualTo(OcrScript.KOREAN)
        assertThat(OcrScriptPolicy.defaultFor(Locale.forLanguageTag("hi"))).isEqualTo(OcrScript.DEVANAGARI)
    }

    @Test
    fun everyOtherLocaleStartsWithLatin() {
        listOf("pt-BR", "en", "es", "fr", "it", "de", "id", "tr").forEach { tag ->
            assertThat(OcrScriptPolicy.defaultFor(Locale.forLanguageTag(tag))).isEqualTo(OcrScript.LATIN)
        }
    }

    @Test
    fun arabicInterfaceNeverMapsToAnUnsupportedRecognizer() {
        assertThat(OcrScriptPolicy.defaultFor(Locale.forLanguageTag("ar"))).isEqualTo(OcrScript.LATIN)
    }
}
