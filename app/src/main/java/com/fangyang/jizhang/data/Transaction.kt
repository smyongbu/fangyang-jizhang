package com.fangyang.jizhang.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一笔账。存在 Room 数据库的 transactions 表里。 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,            // 金额，永远存正数
    val type: TransactionType,     // 收入 / 支出
    val category: String,          // 分类名，如 "餐饮"
    val note: String = "",         // 备注
    val date: Long,                // 发生时间，epoch 毫秒
)
