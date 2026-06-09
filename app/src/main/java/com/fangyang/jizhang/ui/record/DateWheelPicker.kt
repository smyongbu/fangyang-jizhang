package com.fangyang.jizhang.ui.record

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fangyang.jizhang.util.localDateToMillis
import com.fangyang.jizhang.util.millisToLocalDate
import java.time.LocalDate
import java.time.YearMonth

/**
 * 年 / 月 / 日 三列等宽并排在一个框里，默认选中传入日期（通常是今天）。
 * 位置固定不变，点最下面「确定」即可。
 */
@Composable
fun DatePickerWheelDialog(
    title: String,
    initialMillis: Long,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val initial = remember(initialMillis) { millisToLocalDate(initialMillis) }
    val years = remember(today) { (today.year - 3..today.year + 3).toList() }

    var year by remember { mutableIntStateOf(initial.year.coerceIn(years.first(), years.last())) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    var day by remember { mutableIntStateOf(initial.dayOfMonth) }

    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    if (day > daysInMonth) day = daysInMonth

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                ) {
                    WheelColumn("年", years, year, Modifier.weight(1f)) { year = it }
                    WheelColumn("月", (1..12).toList(), month, Modifier.weight(1f)) { month = it }
                    WheelColumn("日", (1..daysInMonth).toList(), day, Modifier.weight(1f)) { day = it }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSelected(localDateToMillis(LocalDate.of(year, month, day))) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("确定") }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    label: String,
    values: List<Int>,
    selected: Int,
    modifier: Modifier = Modifier,
    onPick: (Int) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        val state = rememberLazyListState()
        LaunchedEffect(Unit) {
            state.scrollToItem(values.indexOf(selected).coerceAtLeast(0))
        }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(values) { v ->
                val isSel = v == selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(v) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$v",
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isSel) 19.sp else 16.sp,
                        color = if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
