package com.fangyang.jizhang.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 一条进货记录。 */
@Entity(tableName = "product_records")
data class ProductRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,          // 条形码
    val name: String,             // 商品名
    val purchaseDate: Long,       // 进货日期（epoch 毫秒）
    val productionDate: Long,     // 生产日期
    val expiryDate: Long,         // 过期日期
    val purchasePrice: Double,    // 进货价格
    val retailPrice: Double,      // 零售价格
    val recordDate: Long,         // 记录创建时间，用于查询排序
)
