package com.fangyang.jizhang.data.sync

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** 解析出的令牌。 */
data class TokenResult(
    val accessToken: String,
    val refreshToken: String?,   // 刷新时可能没有新的
    val expiresInSec: Long,
)

/**
 * Dropbox OAuth2 PKCE 登录（手动粘贴授权码方式，无需 redirect URI / app secret）。
 */
object DropboxAuth {

    private const val AUTHORIZE_URL = "https://www.dropbox.com/oauth2/authorize"
    private const val TOKEN_URL = "https://api.dropboxapi.com/oauth2/token"

    fun newCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    /** 用户在浏览器打开这个地址授权，授权后页面会显示一串授权码让用户复制。 */
    fun authorizeUrl(codeChallenge: String): String =
        "$AUTHORIZE_URL?client_id=${DropboxConfig.APP_KEY}" +
            "&response_type=code" +
            "&token_access_type=offline" +
            "&code_challenge=$codeChallenge" +
            "&code_challenge_method=S256"

    suspend fun exchangeCode(client: OkHttpClient, code: String, verifier: String): TokenResult =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("code", code.trim())
                .add("grant_type", "authorization_code")
                .add("client_id", DropboxConfig.APP_KEY)
                .add("code_verifier", verifier)
                .build()
            postToken(client, body)
        }

    suspend fun refresh(client: OkHttpClient, refreshToken: String): TokenResult =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", DropboxConfig.APP_KEY)
                .build()
            postToken(client, body)
        }

    private fun postToken(client: OkHttpClient, body: FormBody): TokenResult {
        val request = Request.Builder().url(TOKEN_URL).post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("Dropbox 登录失败(${resp.code})：$text")
            }
            val json = JSONObject(text)
            return TokenResult(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").ifBlank { null },
                expiresInSec = json.optLong("expires_in", 14400L),
            )
        }
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
