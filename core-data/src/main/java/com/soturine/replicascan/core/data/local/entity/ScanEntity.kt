package com.soturine.replicascan.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "scans",
    indices = [androidx.room.Index(value = ["searchRowId"], unique = true)],
)
data class ScanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: String,
    val tags: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val isDraft: Boolean,
    val searchRowId: Long = 0,
)

