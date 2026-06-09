package com.fangyang.jizhang.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 条形码与商品名的绑定关系。一个条形码只能绑定一个商品名。 */
@Entity(tableName = "barcode_bindings")
data class BarcodeBinding(
    @PrimaryKey val barcode: String,
    val name: String,
)
