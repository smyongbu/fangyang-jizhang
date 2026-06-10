package com.fangyang.jizhang.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fangyang.jizhang.FangYangApp
import com.fangyang.jizhang.data.sync.CloudVersion
import com.fangyang.jizhang.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 可选的自动同步间隔（分钟）。 */
val SYNC_INTERVAL_OPTIONS = listOf(15L, 30L, 60L, 180L, 360L, 720L, 1440L)

fun formatInterval(minutes: Long): String =
    if (minutes < 60) "${minutes}分钟" else "${minutes / 60}小时"

sealed interface SyncDialog {
    data object None : SyncDialog
    data object Conflict : SyncDialog                       // 云端+本地都有数据
    data class CloudOnly(val versions: List<CloudVersion>) : SyncDialog  // 只有云端有数据
}

data class SyncUiState(
    val connected: Boolean = false,
    val autoSync: Boolean = false,
    val autoSyncInterval: Long = 60L,
    val lastSync: Long = 0L,
    val savedUrl: String = "",
    val savedUsername: String = "",
    val busy: Boolean = false,
    val message: String? = null,
    val dialog: SyncDialog = SyncDialog.None,
)

class SyncViewModel(private val manager: SyncManager) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        syncStatus()
    }

    private fun syncStatus(message: String? = null, extra: SyncUiState.() -> SyncUiState = { this }) {
        _state.update {
            it.copy(
                connected = manager.isConnected,
                autoSync = manager.autoSyncEnabled,
                autoSyncInterval = manager.autoSyncIntervalMinutes,
                lastSync = manager.lastSyncMillis,
                savedUrl = manager.savedBaseUrl,
                savedUsername = manager.savedUsername,
                busy = false,
                message = message,
            ).extra()
        }
    }

    // ---- 连接 ----

    fun connect(url: String, username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            syncStatus("请填写账号和应用密码")
            return
        }
        launchBusy {
            manager.connect(url, username, password)
            val cloud = manager.listVersions()
            val localEmpty = manager.isLocalEmpty()
            when {
                cloud.isEmpty() -> {
                    manager.uploadCurrent()
                    syncStatus("已连接，本地数据已备份到坚果云")
                }
                localEmpty -> syncStatus { copy(dialog = SyncDialog.CloudOnly(cloud)) }
                else -> syncStatus { copy(dialog = SyncDialog.Conflict) }
            }
        }
    }

    fun disconnect() {
        manager.disconnect()
        syncStatus("已断开")
    }

    // ---- 同步 ----

    fun syncNow() = launchBusy {
        manager.syncMerge()
        syncStatus("同步完成")
    }

    fun chooseMerge() = launchBusy {
        manager.syncMerge()
        syncStatus("已合并并同步") { copy(dialog = SyncDialog.None) }
    }

    fun chooseKeepCloud() = launchBusy {
        manager.keepCloud()
        syncStatus("已用云端数据覆盖本地") { copy(dialog = SyncDialog.None) }
    }

    fun chooseKeepLocal() = launchBusy {
        manager.keepLocal()
        syncStatus("已用本地数据覆盖云端") { copy(dialog = SyncDialog.None) }
    }

    fun downloadVersion(name: String) = launchBusy {
        manager.keepCloud(name)
        syncStatus("已下载该版本数据") { copy(dialog = SyncDialog.None) }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = SyncDialog.None) }
    }

    fun setAutoSync(enabled: Boolean) {
        manager.setAutoSync(enabled)
        syncStatus(if (enabled) "已开启自动同步" else "已关闭自动同步")
    }

    fun setAutoSyncInterval(minutes: Long) {
        manager.setAutoSyncInterval(minutes)
        syncStatus("同步间隔已设为 ${formatInterval(minutes)}")
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                block()
            } catch (e: Exception) {
                syncStatus("出错：${e.message}")
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FangYangApp
                SyncViewModel(app.syncManager)
            }
        }
    }
}
