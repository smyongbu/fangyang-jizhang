package com.fangyang.jizhang.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 设置页：菜单形式，点「修改商品名」才进入编辑页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenBindings: () -> Unit, onOpenSync: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            SettingEntry(
                icon = Icons.Default.Edit,
                title = "修改商品名",
                subtitle = "更改条形码绑定的商品名字",
                onClick = onOpenBindings,
            )
            SettingEntry(
                icon = Icons.Default.CloudSync,
                title = "Dropbox 同步",
                subtitle = "登录 Dropbox，自动备份/同步数据",
                onClick = onOpenSync,
            )
        }
    }
}

@Composable
private fun SettingEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
