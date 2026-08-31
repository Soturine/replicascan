package com.soturine.replicascan.core.data.local

import android.content.Context
import androidx.room.Room

object ReplicaScanDatabaseFactory {
    fun create(context: Context): ReplicaScanDatabase = Room.databaseBuilder(
        context.applicationContext,
        ReplicaScanDatabase::class.java,
        "replicascan.db",
    ).addMigrations(MIGRATION_1_2).build()
}
