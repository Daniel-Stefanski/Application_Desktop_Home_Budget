package com.example.homebudget.work.worker

import com.example.homebudget.data.sync.DashboardSyncManager
import com.example.homebudget.data.sync.SupabaseToastBus
import com.example.homebudget.data.sync.SyncProcessor
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object WorkSchedulerSupabase {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    fun scheduleSupabaseSync() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            var retryDelayMillis = 30_000L

            while (isActive) {
                if (Prefs.getSupabaseUid().isNullOrBlank()) return@launch

                val pushed = SyncProcessor.processPendingSync()
                if (pushed) {
                    val pulled = DashboardSyncManager.sync()
                    if (pulled) {
                        SupabaseToastBus.showSynced()
                    }
                    return@launch
                }

                SupabaseToastBus.showQueued()
                delay(retryDelayMillis)
                retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(5 * 60_000L)
            }
        }
    }
}
