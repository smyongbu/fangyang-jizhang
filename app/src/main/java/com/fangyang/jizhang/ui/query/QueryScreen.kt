package com.fangyang.jizhang.ui.query

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

    // 最新的在最下面，进入后滚到底部
    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) listState.scrollToItem(records.size - 1)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("查询") }) },
    ) { padding ->
        if (records.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(records, key = { it.id }) { record ->
                    RecordCard(record)
                }
            }
        }
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
