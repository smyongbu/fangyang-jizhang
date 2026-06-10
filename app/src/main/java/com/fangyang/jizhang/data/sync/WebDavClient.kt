package com.fangyang.jizhang.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder

/** 云端一个备份版本。 */
data class CloudVersion(
    val name: String,        // 文件名 snapshot-xxx.json
    val syncedAt: Long,      // 从文件名解析的时间戳
    val sizeBytes: Long,
)

/** WebDAV 读写（HTTP Basic 认证）。坚果云等通用。 */
class WebDavClient(
    private val client: OkHttpClient,
    private val store: WebDavStore,
) {
    private val xml = "application/xml".toMediaType()
    private val octet = "application/octet-stream".toMediaType()

    private fun folderUrl(): String =
        store.baseUrl.trimEnd('/') + "/" + WebDavConfig.FOLDER + "/"

    private fun auth(): String = Credentials.basic(store.username.orEmpty(), store.password.orEmpty())

    /** 验证账号密码是否可用（对根目录 PROPFIND）。 */
    suspend fun testConnection(): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(store.baseUrl.trimEnd('/') + "/")
            .header("Authorization", auth())
            .header("Depth", "0")
            .method("PROPFIND", ByteArray(0).toRequestBody(xml))
            .build()
        client.newCall(request).execute().use { resp ->
            if (resp.code == 401) throw RuntimeException("账号或应用密码不对")
            if (!resp.isSuccessful && resp.code != 207) {
                throw RuntimeException("连接失败(${resp.code})")
            }
        }
    }

    /** 确保备份文件夹存在（MKCOL，已存在会返回 405，忽略）。 */
    suspend fun ensureFolder(): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(folderUrl())
            .header("Authorization", auth())
            .method("MKCOL", ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(request).execute().use { /* 201 创建成功 / 405 已存在，都算 OK */ }
    }

    suspend fun upload(name: String, content: ByteArray): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(folderUrl() + name)
            .header("Authorization", auth())
            .put(content.toRequestBody(octet))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("上传失败(${resp.code})")
        }
    }

    suspend fun download(name: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(folderUrl() + name)
            .header("Authorization", auth())
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("下载失败(${resp.code})")
            resp.body?.bytes() ?: ByteArray(0)
        }
    }

    /** 列出备份文件夹里所有备份版本，按时间升序。 */
    suspend fun listVersions(): List<CloudVersion> = withContext(Dispatchers.IO) {
        val body = """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:getcontentlength/></d:prop></d:propfind>"""
        val request = Request.Builder()
            .url(folderUrl())
            .header("Authorization", auth())
            .header("Depth", "1")
            .method("PROPFIND", body.toRequestBody(xml))
            .build()
        client.newCall(request).execute().use { resp ->
            if (resp.code == 404) return@withContext emptyList()
            if (!resp.isSuccessful && resp.code != 207) {
                throw RuntimeException("读取云端列表失败(${resp.code})")
            }
            parseVersions(resp.body?.string().orEmpty())
        }
    }

    private fun parseVersions(responseXml: String): List<CloudVersion> {
        val blocks = Regex("<[a-zA-Z]*:?response[\\s\\S]*?</[a-zA-Z]*:?response>").findAll(responseXml)
        val hrefRegex = Regex("<[a-zA-Z]*:?href>([^<]+)</[a-zA-Z]*:?href>")
        val sizeRegex = Regex("<[a-zA-Z]*:?getcontentlength>([0-9]+)</")
        val result = mutableListOf<CloudVersion>()
        for (block in blocks) {
            val href = hrefRegex.find(block.value)?.groupValues?.get(1) ?: continue
            val rawName = href.trimEnd('/').substringAfterLast('/')
            val name = runCatching { URLDecoder.decode(rawName, "UTF-8") }.getOrDefault(rawName)
            if (!name.startsWith(WebDavConfig.SNAPSHOT_PREFIX) || !name.endsWith(WebDavConfig.SNAPSHOT_SUFFIX)) {
                continue
            }
            val ts = name
                .removePrefix(WebDavConfig.SNAPSHOT_PREFIX)
                .removeSuffix(WebDavConfig.SNAPSHOT_SUFFIX)
                .toLongOrNull() ?: 0L
            val size = sizeRegex.find(block.value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            result.add(CloudVersion(name, ts, size))
        }
        return result.sortedBy { it.syncedAt }
    }
}
