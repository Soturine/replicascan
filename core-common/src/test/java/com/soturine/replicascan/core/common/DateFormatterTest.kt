package com.soturine.replicascan.core.common

import com.google.common.truth.Truth.assertThat
import com.soturine.replicascan.core.common.util.DateFormatter
import java.util.Locale
import java.util.TimeZone
import org.junit.Test

class DateFormatterTest {
    @Test
    fun `formats date using the requested locale and time zone`() {
        val timestamp = 1_775_743_200_000L
        val zone = TimeZone.getTimeZone("America/Sao_Paulo")
        val ptBr = DateFormatter(Locale.forLanguageTag("pt-BR"), zone).format(timestamp)
        val enUs = DateFormatter(Locale.US, zone).format(timestamp)

        assertThat(ptBr).contains("abr")
        assertThat(ptBr).contains("11:00")
        assertThat(enUs).contains("Apr")
        assertThat(enUs).contains("11:00")
    }
}
