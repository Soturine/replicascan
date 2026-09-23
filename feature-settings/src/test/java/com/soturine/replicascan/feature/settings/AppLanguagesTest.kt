package com.soturine.replicascan.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun everyOptionHasAVisibleUniqueLabel() {
        val tags = AppLanguages.all.map { it.tag }
        assertThat(tags).containsNoDuplicates()
        assertThat(tags).doesNotContain(AppLanguages.SYSTEM_TAG)
        assertThat(AppLanguages.all.map { it.autonym.trim() }).doesNotContain("")
        assertThat(AppLanguages.all).hasSize(12)
    }

    @Test
    fun emptyStoredValueSelectsSystemOnly() {
        assertThat(AppLanguages.resolve("")).isEqualTo(AppLanguages.SYSTEM_TAG)
    }

    @Test
    fun regionalOrListedTagsAlwaysResolveToOneOption() {
        assertThat(AppLanguages.resolve("pt-BR")).isEqualTo("pt-BR")
        assertThat(AppLanguages.resolve("pt-br")).isEqualTo("pt-BR")
        assertThat(AppLanguages.resolve("en-US")).isEqualTo("en")
        assertThat(AppLanguages.resolve("ko-KR,en")).isEqualTo("ko")
        assertThat(AppLanguages.resolve("pt-PT")).isEqualTo("pt-BR")
    }

    @Test
    fun unsupportedLanguageFallsBackToSystemInsteadOfBlank() {
        assertThat(AppLanguages.resolve("sv-SE")).isEqualTo(AppLanguages.SYSTEM_TAG)
    }
}
