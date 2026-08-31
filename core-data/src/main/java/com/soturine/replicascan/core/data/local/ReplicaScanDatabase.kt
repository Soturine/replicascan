package com.soturine.replicascan.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soturine.replicascan.core.data.local.dao.ScanDao
import com.soturine.replicascan.core.data.local.entity.PageEntity
import com.soturine.replicascan.core.data.local.entity.ScanEntity
import com.soturine.replicascan.core.data.local.entity.PageOcrArtifactEntity
import com.soturine.replicascan.core.data.local.entity.ScanSearchFtsEntity

@Database(
    entities = [
        ScanEntity::class,
        PageEntity::class,
        PageOcrArtifactEntity::class,
        ScanSearchFtsEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ReplicaScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}

