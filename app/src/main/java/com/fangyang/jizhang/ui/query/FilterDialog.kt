package com.fangyang.jizhang.ui.query

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fangyang.jizhang.ui.record.DatePickerWheelDialog
import com.fangyang.jizhang.util.formatAmount
import com.fangyang.jizhang.util.formatYmd
import com.fangyang.jizhang.util.todayStartMillis

private val priceRegex = Regex("^\\d*\\.?\\d{0,2}$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    current: QueryFilter,
    onDismiss: () -> Unit,
    onApply: (QueryFilter) -> Unit,
) {
    var field by remember { mutableStateOf(current.field) }
    var startMillis by remember { mutableStateOf(current.startMillis) }
    var endMillis by remember { mutableStateOf(current.endMillis) }
    var minPrice by remember { mutableStateOf(current.minPrice?.let { formatAmount(it) } ?: "") }
    var maxPrice by remember { mutableStateOf(current.maxPrice?.let { formatAmount(it) } ?: "") }
    var datePickerFor by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("限制搜索", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                Text("按条件", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterField.entries.forEach { f ->
                        FilterChip(
                            selected = field == f,
                            onClick = { field = f },
                            label = { Text(f.label) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                when {
                    field == FilterField.NONE -> {
                        Text(
                            "选择一个条件来限制范围",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    field.isDate -> {
                        Text("按时间范围", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        DateButton("起始", startMillis) { datePickerFor = "start" }
                        Spacer(Modifier.height(8.dp))
                        DateButton("结束", endMillis) { datePickerFor = "end" }
                    }

                    else -> {
                        Text("按价格范围", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = minPrice,
                                onValueChange = { if (it.isEmpty() || priceRegex.matches(it)) minPrice = it },
                                label = { Text("最低") },
                                suffix = { Text("¥") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                            Text("  —  ")
                            OutlinedTextField(
                                value = maxPrice,
                                onValueChange = { if (it.isEmpty() || priceRegex.matches(it)) maxPrice = it },
                                label = { Text("最高") },
                                suffix = { Text("¥") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onApply(QueryFilter()) }) { Text("重置") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        val applied = when {
                            field == FilterField.NONE -> QueryFilter()
                            field.isDate -> QueryFilter(field = field, startMillis = startMillis, endMillis = endMillis)
                            else -> QueryFilter(
                                field = field,
                                minPrice = minPrice.toDoubleOrNull(),
                                maxPrice = maxPrice.toDoubleOrNull(),
                            )
                        }
                        onApply(applied)
                    }) { Text("确定") }
                }
            }
        }
    }

    datePickerFor?.let { which ->
        DatePickerWheelDialog(
            title = if (which == "start") "起始日期" else "结束日期",
            initialMillis = (if (which == "start") startMillis else endMillis) ?: todayStartMillis(),
            onDismiss = { datePickerFor = null },
            onSelected = { m ->
                if (which == "start") startMillis = m else endMillis = m
                datePickerFor = null
            },
        )
    }
}

@Composable
private fun DateButton(label: String, millis: Long?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(
            millis?.let { formatYmd(it) } ?: "未设置",
            color = if (millis != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.primary,
        )
    }
}
