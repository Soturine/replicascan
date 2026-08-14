package com.soturine.scanora.core.data.repository

/** Length-prefixed storage keeps commas, pipes, emoji and RTL text lossless. */
internal object TagCodec {
    private const val PREFIX = "tags-v2|"

    fun encode(tags: List<String>): String = buildString {
        append(PREFIX)
        tags.map(String::trim).filter(String::isNotEmpty).distinct().forEach { tag ->
            append(tag.length).append(':').append(tag)
        }
    }

    fun decode(value: String): List<String> {
        if (!value.startsWith(PREFIX)) return value.split('|').map(String::trim).filter(String::isNotEmpty)
        val result = mutableListOf<String>()
        var cursor = PREFIX.length
        while (cursor < value.length) {
            val separator = value.indexOf(':', cursor)
            if (separator < 0) return emptyList()
            val length = value.substring(cursor, separator).toIntOrNull() ?: return emptyList()
            val start = separator + 1
            val end = start + length
            if (length < 0 || end > value.length) return emptyList()
            result += value.substring(start, end)
            cursor = end
        }
        return result
    }
}
