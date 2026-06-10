package com.fangyang.jizhang.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fangyang.jizhang.data.ProductRepository
import com.fangyang.jizhang.data.Snapshot
import com.fangyang.jizhang.data.SnapshotCodec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** WebDAV（坚果云）同步总控：连接、上传/下载、合并/覆盖、自动同步调度。 */
class SyncManager(
    context: Context,
    private val repository: ProductRepository,
) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient()
    private val store = WebDavStore(appContext)
    private val api = WebDavClient(client, store)

    val isConnected: Boolean get() = store.isConnected
    val autoSyncEnabled: Boolean get() = store.autoSync
    val lastSyncMillis: Long get() = store.lastSyncMillis
    val savedBaseUrl: String get() = store.baseUrl
    val savedUsername: String get() = store.username.orEmpty()

    // ---- 连接 ----

    suspend fun connect(url: String, username: String, password: String) {
        store.baseUrl = url.trim().ifBlank { WebDavConfig.DEFAULT_URL }
        store.username = username.trim()
        store.password = password.trim()
        try {
            api.testConnection()
            api.ensureFolder()
        } catch (e: Exception) {
            store.password = null   // 连接失败就别留无效密码
            throw e
        }
    }

    fun disconnect() {
        cancelAutoSync()
        store.clear()
    }

    // ---- 云端版本 ----

    suspend fun listVersions(): List<CloudVersion> = api.listVersions()

    suspend fun downloadSnapshot(name: String): Snapshot =
        SnapshotCodec.fromJson(String(api.download(name), Charsets.UTF_8))

    // ---- 同步操作 ----

    /** 把当前本地数据作为一份新快照上传（保留历史版本）。 */
    suspend fun uploadCurrent(): Long {
        api.ensureFolder()
        val snapshot = repository.exportSnapshot()
        val name = "${WebDavConfig.SNAPSHOT_PREFIX}${snapshot.syncedAt}${WebDavConfig.SNAPSHOT_SUFFIX}"
        api.upload(name, SnapshotCodec.toJson(snapshot).toByteArray(Charsets.UTF_8))
        store.lastSyncMillis = snapshot.syncedAt
        return snapshot.syncedAt
    }

    /** 合并：拉最新云端版本合并进本地，再上传一份新的。 */
    suspend fun syncMerge() {
        listVersions().lastOrNull()?.let { repository.mergeWith(downloadSnapshot(it.name)) }
        uploadCurrent()
    }

    /** 保留云端：用指定（默认最新）云端版本覆盖本地。 */
    suspend fun keepCloud(name: String? = null) {
        val target = name ?: listVersions().lastOrNull()?.name ?: return
        repository.replaceWith(downloadSnapshot(target))
        store.lastSyncMillis = System.currentTimeMillis()
    }

    /** 保留本地：把本地上传成新快照（云端历史仍保留）。 */
    suspend fun keepLocal() {
        uploadCurrent()
    }

    suspend fun isLocalEmpty(): Boolean = repository.isLocalEmpty()

    // ---- 自动同步（WorkManager 周期任务）----

    fun setAutoSync(enabled: Boolean) {
        store.autoSync = enabled
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(appContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            cancelAutoSync()
        }
    }

    private fun cancelAutoSync() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "webdav_auto_sync"
    }
}
