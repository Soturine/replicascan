package com.soturine.replicascan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.soturine.replicascan.core.common.model.DocumentFilterType
import com.soturine.replicascan.core.common.model.PdfQuality

@Composable
fun DocumentFilterType.localizedTitle(): String = stringResource(
    when (this) {
        DocumentFilterType.ORIGINAL -> R.string.filter_original
        DocumentFilterType.ENHANCED -> R.string.filter_enhanced
        DocumentFilterType.GRAYSCALE -> R.string.filter_gray
        DocumentFilterType.BLACK_WHITE -> R.string.filter_bw
    },
)

@Composable
fun PdfQuality.localizedTitle(): String = stringResource(
    when (this) {
        PdfQuality.COMPACT -> R.string.quality_compact
        PdfQuality.BALANCED -> R.string.quality_balanced
        PdfQuality.HIGH -> R.string.quality_high
    },
)

@Composable
fun PdfQuality.localizedDescription(): String = stringResource(
    when (this) {
        PdfQuality.COMPACT -> R.string.quality_compact_description
        PdfQuality.BALANCED -> R.string.quality_balanced_description
        PdfQuality.HIGH -> R.string.quality_high_description
    },
)
