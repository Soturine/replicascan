package com.soturine.replicascan

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.soturine.replicascan.app.AppContainer
import com.soturine.replicascan.core.data.work.CleanupExportsWorker
import java.util.concurrent.TimeUnit

class ReplicaScanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "replicascan_cleanup_exports",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CleanupExportsWorker>(12, TimeUnit.HOURS).build(),
        )
    }
}

