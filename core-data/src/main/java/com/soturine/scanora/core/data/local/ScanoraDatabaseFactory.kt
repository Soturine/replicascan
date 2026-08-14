package com.soturine.scanora.core.data.local

import android.content.Context
import androidx.room.Room

object ScanoraDatabaseFactory {
    fun create(context: Context): ScanoraDatabase = Room.databaseBuilder(
        context.applicationContext,
        ScanoraDatabase::class.java,
        "scanora.db",
    ).addMigrations(MIGRATION_1_2).build()
}
