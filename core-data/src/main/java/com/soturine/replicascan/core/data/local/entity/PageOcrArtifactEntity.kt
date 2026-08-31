package com.soturine.replicascan.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "page_ocr_artifacts",
    primaryKeys = ["pageId"],
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PageOcrArtifactEntity(
    val pageId: String,
    val rawText: String,
    val normalizedText: String,
    val structuredContent: String,
    val script: String,
    val engine: String,
    val engineVersion: String,
    val pipelineVersion: String,
    val sourceFingerprint: String,
    val createdAt: Long,
)
