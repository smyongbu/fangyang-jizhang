package com.fangyang.jizhang.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fangyang.jizhang.FangYangApp

/** 后台周期任务：自动合并同步。 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FangYangApp ?: return Result.failure()
        val manager = app.syncManager
        if (!manager.isConnected) return Result.success()
        return try {
            manager.syncMerge()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
