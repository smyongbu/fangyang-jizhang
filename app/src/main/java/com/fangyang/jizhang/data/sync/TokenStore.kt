package com.fangyang.jizhang.data.sync

import android.content.Context

/** 保存 Dropbox 的令牌和同步设置（SharedPreferences）。 */
class TokenStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("dropbox_sync", Context.MODE_PRIVATE)

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(v) { prefs.edit().putString("refresh_token", v).apply() }

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(v) { prefs.edit().putString("access_token", v).apply() }

    /** access token 过期的时间点（epoch 毫秒）。 */
    var accessExpiryMillis: Long
        get() = prefs.getLong("access_expiry", 0L)
        set(v) { prefs.edit().putLong("access_expiry", v).apply() }

    var autoSync: Boolean
        get() = prefs.getBoolean("auto_sync", false)
        set(v) { prefs.edit().putBoolean("auto_sync", v).apply() }

    var lastSyncMillis: Long
        get() = prefs.getLong("last_sync", 0L)
        set(v) { prefs.edit().putLong("last_sync", v).apply() }

    val isConnected: Boolean get() = refreshToken != null

    fun clear() {
        prefs.edit().clear().apply()
    }
}
