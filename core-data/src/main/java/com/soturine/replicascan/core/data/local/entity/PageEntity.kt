package com.soturine.replicascan.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["scanId"]),
        Index(value = ["scanId", "pageIndex"], unique = true),
    ],
)
data class PageEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val pageIndex: Int,
    val sourceUri: String,
    val processedUri: String?,
    val filterType: String,
    val rotationDegrees: Int,
    val quad: String?,
    val ocrText: String?,
)

