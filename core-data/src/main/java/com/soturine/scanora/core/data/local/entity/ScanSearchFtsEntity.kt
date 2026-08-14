package com.soturine.scanora.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "scan_search_fts")
data class ScanSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val scanId: String,
    val title: String,
    val tags: String,
    val ocrText: String,
)
