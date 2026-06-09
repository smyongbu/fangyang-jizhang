package com.fangyang.jizhang.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fangyang.jizhang.util.localDateToMillis
import com.fangyang.jizhang.util.millisToLocalDate
import java.time.LocalDate
import java.time.YearMonth

private enum class Step { YEAR, MONTH, DAY }

/**
 * 级联日期选择：先选年（今年±3 共7个，默认今年）→ 再选月（12个）→ 再选日。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CascadingDatePickerDialog(
    title: String,
    initialMillis: Long,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val initial = remember(initialMillis) { millisToLocalDate(initialMillis) }

    var step by remember { mutableStateOf(Step.YEAR) }
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }

    val years = remember(today) { (today.year - 3..today.year + 3).toList() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val stepLabel = when (step) {
                    Step.YEAR -> "选择年份"
                    Step.MONTH -> "$year 年 · 选择月份"
                    Step.DAY -> "$year 年 $month 月 · 选择日期"
                }
                Text("$title — $stepLabel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(Modifier.size(16.dp))

                when (step) {
                    Step.YEAR -> OptionGrid(
                        items = years.map { it to "$it" },
                        highlight = today.year,
                    ) { picked ->
                        year = picked
                        step = Step.MONTH
                    }

                    Step.MONTH -> OptionGrid(
                        items = (1..12).map { it to "${it}月" },
                        highlight = month,
                    ) { picked ->
                        month = picked
                        step = Step.DAY
                    }

                    Step.DAY -> {
                        val days = YearMonth.of(year, month).lengthOfMonth()
                        OptionGrid(
                            items = (1..days).map { it to "$it" },
                            highlight = initial.dayOfMonth.takeIf {
                                year == initial.year && month == initial.monthValue
                            } ?: -1,
                        ) { picked ->
                            onSelected(localDateToMillis(LocalDate.of(year, month, picked)))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGrid(
    items: List<Pair<Int, String>>,
    highlight: Int,
    onPick: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (value, label) ->
            val selected = value == highlight
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onPick(value) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
