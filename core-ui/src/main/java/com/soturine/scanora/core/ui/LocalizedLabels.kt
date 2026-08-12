package com.soturine.scanora.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.soturine.scanora.core.common.model.DocumentFilterType
import com.soturine.scanora.core.common.model.PdfQuality
import com.soturine.scanora.core.common.model.ScanMode

@Composable
fun ScanMode.localizedTitle(): String = stringResource(when (this) {
    ScanMode.NOTEBOOK -> R.string.mode_notebook
    ScanMode.DOCUMENT -> R.string.mode_document
    ScanMode.RECEIPT -> R.string.mode_receipt
})

@Composable
fun ScanMode.localizedDescription(): String = stringResource(when (this) {
    ScanMode.NOTEBOOK -> R.string.mode_notebook_description
    ScanMode.DOCUMENT -> R.string.mode_document_description
    ScanMode.RECEIPT -> R.string.mode_receipt_description
})

@Composable
fun DocumentFilterType.localizedTitle(): String = stringResource(when (this) {
    DocumentFilterType.ORIGINAL_CORRECTED -> R.string.filter_original
    DocumentFilterType.DOCUMENT_BLACK_WHITE -> R.string.filter_bw
    DocumentFilterType.DOCUMENT_GRAY -> R.string.filter_gray
    DocumentFilterType.COLOR_ENHANCED -> R.string.filter_color
    DocumentFilterType.RECEIPT_HIGH_CONTRAST -> R.string.filter_receipt
})

@Composable
fun DocumentFilterType.localizedDescription(): String = stringResource(when (this) {
    DocumentFilterType.ORIGINAL_CORRECTED -> R.string.filter_original_description
    DocumentFilterType.DOCUMENT_BLACK_WHITE -> R.string.filter_bw_description
    DocumentFilterType.DOCUMENT_GRAY -> R.string.filter_gray_description
    DocumentFilterType.COLOR_ENHANCED -> R.string.filter_color_description
    DocumentFilterType.RECEIPT_HIGH_CONTRAST -> R.string.filter_receipt_description
})

@Composable
fun PdfQuality.localizedTitle(): String = stringResource(when (this) {
    PdfQuality.COMPACT -> R.string.quality_compact
    PdfQuality.BALANCED -> R.string.quality_balanced
    PdfQuality.HIGH -> R.string.quality_high
})

@Composable
fun PdfQuality.localizedDescription(): String = stringResource(when (this) {
    PdfQuality.COMPACT -> R.string.quality_compact_description
    PdfQuality.BALANCED -> R.string.quality_balanced_description
    PdfQuality.HIGH -> R.string.quality_high_description
})
