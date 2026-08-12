package com.soturine.scanora.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soturine.scanora.core.data.local.dao.ScanDao
import com.soturine.scanora.core.data.local.entity.PageEntity
import com.soturine.scanora.core.data.local.entity.ScanEntity

@Database(
    entities = [
        ScanEntity::class,
        PageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ScanoraDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}

