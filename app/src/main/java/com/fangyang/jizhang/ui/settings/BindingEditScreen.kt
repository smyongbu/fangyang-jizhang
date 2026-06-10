package com.fangyang.jizhang.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangyang.jizhang.data.BarcodeBinding
import com.fangyang.jizhang.ui.ProductViewModel

/** 改商品名页：从设置点进来才能改。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BindingEditScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BarcodeBinding?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改商品名") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "条形码与商品名（点击可改名）",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            if (bindings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有绑定的商品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(bindings, key = { it.barcode }) { binding ->
                        BindingRow(binding) { editing = binding }
                    }
                }
            }
        }
    }

    editing?.let { binding ->
        EditNameDialog(
            binding = binding,
            onDismiss = { editing = null },
            onConfirm = { newName ->
                viewModel.renameProduct(binding.barcode, newName)
                editing = null
            },
        )
    }
}

@Composable
private fun BindingRow(binding: BarcodeBinding, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(binding.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "条形码 ${binding.barcode}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.Edit, contentDescription = "改名", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EditNameDialog(
    binding: BarcodeBinding,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(binding.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改商品名") },
        text = {
            Column {
                Text(
                    "条形码 ${binding.barcode}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("商品名") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
