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

sealed interface SyncDialog {
    data object None : SyncDialog
    data object Conflict : SyncDialog                       // 云端+本地都有数据
    data class CloudOnly(val versions: List<CloudVersion>) : SyncDialog  // 只有云端有数据
}

data class SyncUiState(
    val connected: Boolean = false,
    val autoSync: Boolean = false,
    val lastSync: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
    val awaitingCode: Boolean = false,     // 已打开授权页，等待粘贴授权码
    val openAuthUrl: String? = null,       // 需要打开的授权地址（一次性）
    val dialog: SyncDialog = SyncDialog.None,
)

class SyncViewModel(private val manager: SyncManager) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var verifier: String? = null

    init {
        syncStatus()
    }

    private fun syncStatus(message: String? = null, extra: SyncUiState.() -> SyncUiState = { this }) {
        _state.update {
            it.copy(
                connected = manager.isConnected,
                autoSync = manager.autoSyncEnabled,
                lastSync = manager.lastSyncMillis,
                busy = false,
                message = message,
            ).extra()
        }
    }

    // ---- 登录 ----

    fun startLogin() {
        val v = manager.newVerifier()
        verifier = v
        _state.update { it.copy(openAuthUrl = manager.authorizeUrl(v), awaitingCode = true, message = null) }
    }

    fun onAuthUrlOpened() {
        _state.update { it.copy(openAuthUrl = null) }
    }

    fun completeLogin(code: String) {
        val v = verifier ?: return
        if (code.isBlank()) return
        launchBusy {
            manager.completeLogin(code, v)
            val cloud = manager.listVersions()
            val localEmpty = manager.isLocalEmpty()
            when {
                cloud.isEmpty() -> {
                    manager.uploadCurrent()
                    syncStatus("已连接，本地数据已备份到 Dropbox") { copy(awaitingCode = false) }
                }
                localEmpty -> syncStatus { copy(awaitingCode = false, dialog = SyncDialog.CloudOnly(cloud)) }
                else -> syncStatus { copy(awaitingCode = false, dialog = SyncDialog.Conflict) }
            }
        }
    }

    fun disconnect() {
        manager.disconnect()
        verifier = null
        _state.value = SyncUiState()
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

    fun downloadVersion(path: String) = launchBusy {
        manager.keepCloud(path)
        syncStatus("已下载该版本数据") { copy(dialog = SyncDialog.None) }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = SyncDialog.None) }
    }

    fun setAutoSync(enabled: Boolean) {
        manager.setAutoSync(enabled)
        syncStatus(if (enabled) "已开启自动同步（每隔约 1 小时）" else "已关闭自动同步")
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
