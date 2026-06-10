package com.fangyang.jizhang.ui.sync

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangyang.jizhang.data.sync.CloudVersion
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(viewModel: SyncViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 打开授权地址（一次性）
    LaunchedEffect(state.openAuthUrl) {
        state.openAuthUrl?.let { url ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            viewModel.onAuthUrlOpened()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dropbox 同步") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!state.connected) {
                DisconnectedView(state, viewModel)
            } else {
                ConnectedView(state, viewModel)
            }

            state.message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            if (state.busy) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.fillMaxWidth(0.05f))
                    Text("处理中…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    when (val d = state.dialog) {
        is SyncDialog.Conflict -> ConflictDialog(
            onMerge = viewModel::chooseMerge,
            onKeepCloud = viewModel::chooseKeepCloud,
            onKeepLocal = viewModel::chooseKeepLocal,
            onDismiss = viewModel::dismissDialog,
        )
        is SyncDialog.CloudOnly -> CloudVersionDialog(
            versions = d.versions,
            onPick = viewModel::downloadVersion,
            onDismiss = viewModel::dismissDialog,
        )
        SyncDialog.None -> Unit
    }
}

@Composable
private fun DisconnectedView(state: SyncUiState, viewModel: SyncViewModel) {
    Text("把记账数据同步到你的 Dropbox", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(8.dp))
    Text(
        "App 只能读写它在 Dropbox 里的专属文件夹，碰不到你别的文件。",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))

    Button(onClick = viewModel::startLogin, modifier = Modifier.fillMaxWidth()) {
        Text("连接 Dropbox")
    }

    if (state.awaitingCode) {
        Spacer(Modifier.height(20.dp))
        Text(
            "浏览器里登录并点「允许」后，会显示一串授权码。复制它，粘贴到下面：",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        var code by remember { mutableStateOf("") }
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("授权码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.completeLogin(code) },
            enabled = code.isNotBlank() && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("完成连接") }
    }
}

@Composable
private fun ConnectedView(state: SyncUiState, viewModel: SyncViewModel) {
    Text("已连接 Dropbox", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(Modifier.height(6.dp))
    Text(
        "上次同步：" + if (state.lastSync > 0) formatDateTime(state.lastSync) else "尚未同步",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))

    Button(
        onClick = viewModel::syncNow,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("立即同步（合并）") }

    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("自动同步", fontWeight = FontWeight.Medium)
            Text("每隔约 1 小时在后台合并同步", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = state.autoSync, onCheckedChange = viewModel::setAutoSync)
    }

    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = viewModel::disconnect, modifier = Modifier.fillMaxWidth()) {
        Text("断开连接")
    }
}

@Composable
private fun ConflictDialog(
    onMerge: () -> Unit,
    onKeepCloud: () -> Unit,
    onKeepLocal: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            Text("云端和本地都有数据", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "请选择如何处理：",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = onMerge, modifier = Modifier.fillMaxWidth()) {
                Text("合并数据（推荐，不丢数据）")
            }
            Spacer(Modifier.height(14.dp))

            Text("下面两个是覆盖操作，需按住 3 秒确认：", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            HoldToConfirmButton(text = "保留云端数据（覆盖本地）", onConfirm = onKeepCloud)
            Spacer(Modifier.height(10.dp))
            HoldToConfirmButton(text = "保留本地数据（覆盖云端）", onConfirm = onKeepLocal)

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("取消") }
        }
    }
}

/** 长按 3 秒确认的按钮：按住时进度条填充，松手取消，满 3 秒触发。 */
@Composable
private fun HoldToConfirmButton(
    text: String,
    holdMillis: Long = 3000,
    onConfirm: () -> Unit,
) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(holding) {
        if (holding) {
            val start = System.currentTimeMillis()
            while (holding) {
                progress = ((System.currentTimeMillis() - start) / holdMillis.toFloat()).coerceIn(0f, 1f)
                if (progress >= 1f) {
                    onConfirm()
                    holding = false
                    break
                }
                delay(16)
            }
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    }
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // 进度填充
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.error)
        )
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            Text(
                if (holding) "按住不放…（$text）" else "$text · 按住 3 秒",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun CloudVersionDialog(
    versions: List<CloudVersion>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            Text("云端有数据", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "本地是空的。选择一个云端版本下载到本机：",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.verticalScroll(rememberScrollState()).height(260.dp)) {
                versions.asReversed().forEach { v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(v.path) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatDateTime(v.syncedAt))
                        Text("${v.sizeBytes} B", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Divider()
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("取消") }
        }
    }
}

private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

private fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
