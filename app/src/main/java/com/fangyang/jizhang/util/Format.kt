package com.fangyang.jizhang.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 金额格式化：1234.5 -> "¥1,234.50" */
fun formatMoney(amount: Double): String =
    "¥" + String.format(Locale.CHINA, "%,.2f", amount)

/** 不带符号的金额：用于输入框回显等。 */
fun formatAmount(amount: Double): String =
    String.format(Locale.CHINA, "%.2f", amount)

private val dayFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
private val fullFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

fun formatDay(date: LocalDate): String = date.format(dayFormatter)

fun formatFullDate(date: LocalDate): String = date.format(fullFormatter)
