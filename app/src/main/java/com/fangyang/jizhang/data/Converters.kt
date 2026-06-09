package com.fangyang.jizhang.data

import androidx.room.TypeConverter

/** Room 不会自动存枚举，这里手动转成字符串。 */
class Converters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}
