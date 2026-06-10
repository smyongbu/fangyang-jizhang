package com.fangyang.jizhang.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** 云端一个备份版本。 */
data class CloudVersion(
    val path: String,        // 形如 /snapshot-1718000000000.json
    val syncedAt: Long,      // 从文件名解析的时间戳
    val sizeBytes: Long,
)

/** Dropbox 文件读写（App 专属文件夹）。会在 access token 过期时自动刷新。 */
class DropboxApi(
    private val client: OkHttpClient,
    private val tokens: TokenStore,
) {
    private val octet = "application/octet-stream".toMediaType()

    private suspend fun validAccessToken(): String {
        val now = System.currentTimeMillis()
        val token = tokens.accessToken
        if (token != null && now < tokens.accessExpiryMillis - 60_000) return token

        val refresh = tokens.refreshToken ?: throw IllegalStateException("未连接 Dropbox")
        val result = DropboxAuth.refresh(client, refresh)
        tokens.accessToken = result.accessToken
        tokens.accessExpiryMillis = System.currentTimeMillis() + result.expiresInSec * 1000
        return result.accessToken
    }

    suspend fun upload(path: String, content: ByteArray): Unit = withContext(Dispatchers.IO) {
        val arg = JSONObject()
            .put("path", path)
            .put("mode", "overwrite")
            .put("mute", true)
            .toString()
        val request = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/upload")
            .addHeader("Authorization", "Bearer ${validAccessToken()}")
            .addHeader("Dropbox-API-Arg", arg)
            .post(content.toRequestBody(octet))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("上传失败(${resp.code})：${resp.body?.string()}")
            }
        }
    }

    suspend fun download(path: String): ByteArray = withContext(Dispatchers.IO) {
        val arg = JSONObject().put("path", path).toString()
        val request = Request.Builder()
            .url("https://content.dropboxapi.com/2/files/download")
            .addHeader("Authorization", "Bearer ${validAccessToken()}")
            .addHeader("Dropbox-API-Arg", arg)
            .post(ByteArray(0).toRequestBody(octet))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("下载失败(${resp.code})：${resp.body?.string()}")
            }
            resp.body?.bytes() ?: ByteArray(0)
        }
    }

    /** 列出 App 文件夹里所有备份版本，按时间升序。 */
    suspend fun listVersions(): List<CloudVersion> = withContext(Dispatchers.IO) {
        val arg = JSONObject().put("path", "").toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.dropboxapi.com/2/files/list_folder")
            .addHeader("Authorization", "Bearer ${validAccessToken()}")
            .post(arg)
            .build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("读取云端列表失败(${resp.code})：$text")
            }
            val entries = JSONObject(text).optJSONArray("entries") ?: return@withContext emptyList()
            val result = mutableListOf<CloudVersion>()
            for (i in 0 until entries.length()) {
                val e = entries.getJSONObject(i)
                val name = e.optString("name")
                if (e.optString(".tag") == "file" &&
                    name.startsWith(DropboxConfig.SNAPSHOT_PREFIX) &&
                    name.endsWith(DropboxConfig.SNAPSHOT_SUFFIX)
                ) {
                    val ts = name
                        .removePrefix(DropboxConfig.SNAPSHOT_PREFIX)
                        .removeSuffix(DropboxConfig.SNAPSHOT_SUFFIX)
                        .toLongOrNull() ?: 0L
                    result.add(CloudVersion("/$name", ts, e.optLong("size", 0L)))
                }
            }
            result.sortedBy { it.syncedAt }
        }
    }
}
