package com.soturine.replicascan.navigation

import com.soturine.replicascan.core.common.model.ScanMode

object ReplicaScanDestinations {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val History = "history"
    const val Settings = "settings"
    const val About = "about"
    const val Camera = "camera/{mode}"
    const val Crop = "editor/crop/{scanId}/{pageId}"
    const val Filters = "editor/filters/{scanId}/{pageId}"
    const val Review = "editor/review/{scanId}"
    const val Export = "export/{scanId}"
    const val Detail = "detail/{scanId}"
    const val Ocr = "ocr/{scanId}/{pageId}"

    fun camera(mode: ScanMode): String = "camera/${mode.storageKey}"
    fun crop(scanId: String, pageId: String): String = "editor/crop/$scanId/$pageId"
    fun filters(scanId: String, pageId: String): String = "editor/filters/$scanId/$pageId"
    fun review(scanId: String): String = "editor/review/$scanId"
    fun export(scanId: String): String = "export/$scanId"
    fun detail(scanId: String): String = "detail/$scanId"
    fun ocr(scanId: String, pageId: String): String = "ocr/$scanId/$pageId"
}

