package com.fangyang.jizhang.ui.query

import com.fangyang.jizhang.data.ProductRecord

/** 限制搜索可选的条件字段。 */
enum class FilterField(val label: String, val isDate: Boolean) {
    NONE("不限制", false),
    PURCHASE_DATE("进货日期", true),
    PRODUCTION_DATE("生产日期", true),
    EXPIRY_DATE("过期日期", true),
    PURCHASE_PRICE("进货价格", false),
    RETAIL_PRICE("零售价格", false),
}

/** 查询页的过滤条件：日期字段用时间范围，价格字段用数值范围。 */
data class QueryFilter(
    val field: FilterField = FilterField.NONE,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
) {
    val isActive: Boolean get() = field != FilterField.NONE
}

/** 一条记录是否同时满足关键词和过滤条件。 */
fun ProductRecord.matches(keyword: String, filter: QueryFilter): Boolean {
    val kw = keyword.trim()
    if (kw.isNotEmpty() && !name.contains(kw, ignoreCase = true) && !barcode.contains(kw)) {
        return false
    }
    return when (filter.field) {
        FilterField.NONE -> true
        FilterField.PURCHASE_DATE -> dateIn(purchaseDate, filter.startMillis, filter.endMillis)
        FilterField.PRODUCTION_DATE -> dateIn(productionDate, filter.startMillis, filter.endMillis)
        FilterField.EXPIRY_DATE -> dateIn(expiryDate, filter.startMillis, filter.endMillis)
        FilterField.PURCHASE_PRICE -> priceIn(purchasePrice, filter.minPrice, filter.maxPrice)
        FilterField.RETAIL_PRICE -> priceIn(retailPrice, filter.minPrice, filter.maxPrice)
    }
}

private fun dateIn(value: Long, start: Long?, end: Long?): Boolean {
    if (start != null && value < start) return false
    if (end != null && value > end) return false
    return true
}

private fun priceIn(value: Double, min: Double?, max: Double?): Boolean {
    if (min != null && value < min) return false
    if (max != null && value > max) return false
    return true
}
