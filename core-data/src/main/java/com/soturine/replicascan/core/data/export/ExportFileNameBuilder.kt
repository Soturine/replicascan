package com.soturine.replicascan.core.data.export

import com.soturine.replicascan.core.common.model.ExportFormat
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ExportFileNameBuilder(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val timeZone: TimeZone = TimeZone.getTimeZone("America/Sao_Paulo"),
) {
    fun buildBaseName(
        title: String,
        format: ExportFormat,
        suffix: String? = null,
    ): String {
        val slug = normalize(title)
        val stamp = timestampFormatter().format(currentTimeMillis())
        val optionalSuffix = suffix?.let { "-$it" }.orEmpty()
        return "$slug-$stamp$optionalSuffix.${format.fileExtension}"
    }

    fun buildPageName(
        title: String,
        pageIndex: Int,
        format: ExportFormat,
    ): String = buildBaseName(
        title = title,
        format = format,
        suffix = "p${(pageIndex + 1).toString().padStart(2, '0')}",
    )

    private fun normalize(title: String): String =
        Normalizer.normalize(title.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()
            .ifBlank { "replicascan-scan" }
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(50)
            .ifBlank { "replicascan-scan" }

    private fun timestampFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).apply {
            this.timeZone = this@ExportFileNameBuilder.timeZone
        }
    }
