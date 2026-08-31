package com.soturine.replicascan.core.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.soturine.replicascan.core.data.local.entity.PageEntity
import com.soturine.replicascan.core.data.local.entity.ScanEntity

data class ScanWithPages(
    @Embedded val scan: ScanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scanId",
    )
    val pages: List<PageEntity>,
)

