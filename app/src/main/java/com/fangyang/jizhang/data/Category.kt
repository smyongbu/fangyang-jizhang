package com.fangyang.jizhang.data

/** 一个记账分类，emoji 当作图标，省去图片资源。 */
data class Category(
    val name: String,
    val emoji: String,
    val type: TransactionType,
)

/** 预置分类。后续可以改成存数据库、让用户自定义。 */
object Categories {
    val expense = listOf(
        Category("餐饮", "🍜", TransactionType.EXPENSE),
        Category("交通", "🚗", TransactionType.EXPENSE),
        Category("购物", "🛍️", TransactionType.EXPENSE),
        Category("娱乐", "🎮", TransactionType.EXPENSE),
        Category("居家", "🏠", TransactionType.EXPENSE),
        Category("医疗", "💊", TransactionType.EXPENSE),
        Category("教育", "📚", TransactionType.EXPENSE),
        Category("人情", "🎁", TransactionType.EXPENSE),
        Category("旅行", "✈️", TransactionType.EXPENSE),
        Category("其他", "📦", TransactionType.EXPENSE),
    )

    val income = listOf(
        Category("工资", "💰", TransactionType.INCOME),
        Category("奖金", "🏆", TransactionType.INCOME),
        Category("理财", "📈", TransactionType.INCOME),
        Category("兼职", "💼", TransactionType.INCOME),
        Category("红包", "🧧", TransactionType.INCOME),
        Category("其他", "📦", TransactionType.INCOME),
    )

    fun forType(type: TransactionType): List<Category> =
        if (type == TransactionType.INCOME) income else expense

    /** 按分类名找 emoji，找不到给个默认。 */
    fun emojiOf(name: String): String =
        (expense + income).firstOrNull { it.name == name }?.emoji ?: "📦"
}
