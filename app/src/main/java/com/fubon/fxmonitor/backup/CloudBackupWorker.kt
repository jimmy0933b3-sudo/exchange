package com.fubon.fxmonitor.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val manager: CloudBackupManager
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!manager.state.value.enabled || manager.state.value.email.isNullOrBlank()) return Result.success()
        return try {
            manager.backupNow()
            Result.success()
        } catch (e: Exception) {
            manager.reportError(e.message ?: "背景備份失敗")
            if (e is IOException && runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
