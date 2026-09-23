package com.soturine.replicascan.feature.settings

/**
 * Supported interface languages, labelled with their autonyms so people can find their own
 * language regardless of the current one. The empty tag means “follow the system”.
 */
data class AppLanguage(val tag: String, val autonym: String)

object AppLanguages {
    const val SYSTEM_TAG = ""

    val all: List<AppLanguage> = listOf(
        AppLanguage("pt-BR", "Português (Brasil)"),
        AppLanguage("en", "English"),
        AppLanguage("es", "Español"),
        AppLanguage("fr", "Français"),
        AppLanguage("it", "Italiano"),
        AppLanguage("ar", "العربية"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("ja", "日本語"),
        AppLanguage("ko", "한국어"),
    )

    /**
     * Maps whatever AppCompat reports (`"en-US"`, `"pt-BR,en"`, `""`) to exactly one option, so
     * the picker never shows a list without a selected entry.
     */
    fun resolve(storedTags: String): String {
        val primary = storedTags.substringBefore(',').trim()
        if (primary.isEmpty()) return SYSTEM_TAG
        all.firstOrNull { it.tag.equals(primary, ignoreCase = true) }?.let { return it.tag }
        val language = primary.substringBefore('-').lowercase()
        return all.firstOrNull { it.tag.substringBefore('-').lowercase() == language }?.tag ?: SYSTEM_TAG
    }

    fun autonym(tag: String): String? = all.firstOrNull { it.tag == tag }?.autonym
}
