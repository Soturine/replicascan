package com.soturine.scanora.core.common.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateFormatter(
    private val locale: Locale = Locale.getDefault(),
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {
    fun format(timestamp: Long): String {
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
            timeZone = this@DateFormatter.timeZone
        }.format(Date(timestamp))
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).apply {
            timeZone = this@DateFormatter.timeZone
        }.format(Date(timestamp))
        return "$date • $time"
    }
}
