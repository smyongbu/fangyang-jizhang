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

/** Dropbox 同步的总控：登录、上传/下载、合并/覆盖、自动同步调度。 */
class SyncManager(
    context: Context,
    private val repository: ProductRepository,
) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient()
    val tokens = TokenStore(appContext)
    private val api = DropboxApi(client, tokens)

    val isConnected: Boolean get() = tokens.isConnected
    val autoSyncEnabled: Boolean get() = tokens.autoSync
    val lastSyncMillis: Long get() = tokens.lastSyncMillis

    // ---- 登录（PKCE 手动粘贴授权码）----

    fun newVerifier(): String = DropboxAuth.newCodeVerifier()

    fun authorizeUrl(verifier: String): String =
        DropboxAuth.authorizeUrl(DropboxAuth.codeChallenge(verifier))

    suspend fun completeLogin(code: String, verifier: String) {
        val result = DropboxAuth.exchangeCode(client, code, verifier)
        tokens.refreshToken = result.refreshToken
            ?: throw RuntimeException("没拿到刷新令牌，请重新授权一次")
        tokens.accessToken = result.accessToken
        tokens.accessExpiryMillis = System.currentTimeMillis() + result.expiresInSec * 1000
    }

    fun disconnect() {
        cancelAutoSync()
        tokens.clear()
    }

    // ---- 云端版本 ----

    suspend fun listVersions(): List<CloudVersion> = api.listVersions()

    suspend fun hasCloudData(): Boolean = api.listVersions().isNotEmpty()

    suspend fun downloadSnapshot(path: String): Snapshot =
        SnapshotCodec.fromJson(String(api.download(path), Charsets.UTF_8))

    // ---- 同步操作 ----

    /** 把当前本地数据作为一份新快照上传（保留历史版本）。 */
    suspend fun uploadCurrent(): Long {
        val snapshot = repository.exportSnapshot()
        val path = "/${DropboxConfig.SNAPSHOT_PREFIX}${snapshot.syncedAt}${DropboxConfig.SNAPSHOT_SUFFIX}"
        api.upload(path, SnapshotCodec.toJson(snapshot).toByteArray(Charsets.UTF_8))
        tokens.lastSyncMillis = snapshot.syncedAt
        return snapshot.syncedAt
    }

    /** 合并：拉最新云端版本合并进本地，再上传一份新的。 */
    suspend fun syncMerge() {
        listVersions().lastOrNull()?.let { repository.mergeWith(downloadSnapshot(it.path)) }
        uploadCurrent()
    }

    /** 保留云端：用指定（默认最新）云端版本覆盖本地。 */
    suspend fun keepCloud(path: String? = null) {
        val target = path ?: listVersions().lastOrNull()?.path ?: return
        repository.replaceWith(downloadSnapshot(target))
        tokens.lastSyncMillis = System.currentTimeMillis()
    }

    /** 保留本地：把本地上传成新快照（云端历史仍保留）。 */
    suspend fun keepLocal() {
        uploadCurrent()
    }

    suspend fun isLocalEmpty(): Boolean = repository.isLocalEmpty()

    // ---- 自动同步（WorkManager 周期任务）----

    fun setAutoSync(enabled: Boolean) {
        tokens.autoSync = enabled
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
        const val WORK_NAME = "dropbox_auto_sync"
    }
}
