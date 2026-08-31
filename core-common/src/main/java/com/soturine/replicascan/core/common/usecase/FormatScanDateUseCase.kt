package com.soturine.replicascan.core.common.usecase

import com.soturine.replicascan.core.common.util.DateFormatter

class FormatScanDateUseCase(
    private val dateFormatter: DateFormatter = DateFormatter(),
) {
    operator fun invoke(timestamp: Long): String = dateFormatter.format(timestamp)
}

