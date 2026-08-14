package com.soturine.scanora.core.data

import com.google.common.truth.Truth.assertThat
import com.soturine.scanora.core.data.repository.TagCodec
import org.junit.Test

class TagCodecTest {
    @Test
    fun preservesSeparatorsUnicodeAndRtl() {
        val tags = listOf("tax|2026", "東京", "فاتورة", "comma, safe")
        assertThat(TagCodec.decode(TagCodec.encode(tags))).containsExactlyElementsIn(tags).inOrder()
    }

    @Test
    fun readsLegacyPipeSeparatedTags() {
        assertThat(TagCodec.decode("tax|2026")).containsExactly("tax", "2026").inOrder()
    }
}
