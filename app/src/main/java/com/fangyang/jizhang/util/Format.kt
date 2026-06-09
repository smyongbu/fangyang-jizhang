package com.fangyang.jizhang.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 金额格式化：1234.5 -> "1,234.50"（符号由界面单独加 ¥） */
fun formatAmount(amount: Double): String =
    String.format(Locale.CHINA, "%.2f", amount)

private val ymdFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/** 统一的年月日显示。 */
fun formatYmd(date: LocalDate): String = date.format(ymdFormatter)

fun formatYmd(millis: Long): String = formatYmd(millisToLocalDate(millis))

// ---- 日期与毫秒互转（按本地时区当天 0 点）----

fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun todayStartMillis(): Long = localDateToMillis(LocalDate.now())
