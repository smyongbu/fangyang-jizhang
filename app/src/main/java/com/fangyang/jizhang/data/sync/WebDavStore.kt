package com.fangyang.jizhang.data.sync

import android.content.Context

/** 保存 WebDAV（坚果云）连接信息和同步设置。 */
class WebDavStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("webdav_sync", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", WebDavConfig.DEFAULT_URL) ?: WebDavConfig.DEFAULT_URL
        set(v) { prefs.edit().putString("base_url", v).apply() }

    var username: String?
        get() = prefs.getString("username", null)
        set(v) { prefs.edit().putString("username", v).apply() }

    var password: String?
        get() = prefs.getString("password", null)
        set(v) { prefs.edit().putString("password", v).apply() }

    var autoSync: Boolean
        get() = prefs.getBoolean("auto_sync", false)
        set(v) { prefs.edit().putBoolean("auto_sync", v).apply() }

    /** 自动同步间隔（分钟）。WorkManager 最小 15 分钟。 */
    var autoSyncIntervalMinutes: Long
        get() = prefs.getLong("auto_interval", 60L)
        set(v) { prefs.edit().putLong("auto_interval", v).apply() }

    var lastSyncMillis: Long
        get() = prefs.getLong("last_sync", 0L)
        set(v) { prefs.edit().putLong("last_sync", v).apply() }

    val isConnected: Boolean get() = !username.isNullOrBlank() && !password.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
