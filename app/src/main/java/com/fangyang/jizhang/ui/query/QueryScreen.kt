package com.fangyang.jizhang.ui.query

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fangyang.jizhang.data.ProductRecord
import com.fangyang.jizhang.ui.ProductViewModel
import com.fangyang.jizhang.util.formatAmount
import com.fangyang.jizhang.util.formatYmd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryScreen(viewModel: ProductViewModel) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var keyword by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(QueryFilter()) }
    var showFilter by remember { mutableStateOf(false) }

    val filtered = remember(records, keyword, filter) {
        records.filter { it.matches(keyword, filter) }
    }

    // 默认（无搜索/过滤）时，最新的在最下面并滚到底部
    val isDefaultView = keyword.isBlank() && !filter.isActive
    LaunchedEffect(filtered.size, isDefaultView) {
        if (isDefaultView && filtered.isNotEmpty()) listState.scrollToItem(filtered.size - 1)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("查询") }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    placeholder = { Text("搜索商品名 / 条形码") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showFilter = true }) {
                    BadgedBox(badge = { if (filter.isActive) Badge() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "限制搜索")
                    }
                }
            }

            if (filter.isActive) {
                Text(
                    "已限制：${filter.field.label}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (records.isEmpty()) "还没有记录" else "没有符合条件的记录",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { record -> RecordCard(record) }
                }
            }
        }
    }

    if (showFilter) {
        FilterDialog(
            current = filter,
            onDismiss = { showFilter = false },
            onApply = { filter = it; showFilter = false },
        )
    }
}

@Composable
private fun RecordCard(record: ProductRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "记录于 ${formatYmd(record.recordDate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "条形码 ${record.barcode}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoLine("进货日期", formatYmd(record.purchaseDate))
            InfoLine("生产日期", formatYmd(record.productionDate))
            InfoLine("过期日期", formatYmd(record.expiryDate))
            InfoLine("进货价格", "¥${formatAmount(record.purchasePrice)}")
            InfoLine("零售价格", "¥${formatAmount(record.retailPrice)}")
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp)
    }
}
