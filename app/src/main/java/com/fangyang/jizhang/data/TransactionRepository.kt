package com.fangyang.jizhang.data

import kotlinx.coroutines.flow.Flow

/**
 * 数据仓库。界面层只跟它打交道，不直接碰数据库。
 * 以后要加云同步，在这一层接入即可，上层无需改动。
 */
class TransactionRepository(private val dao: TransactionDao) {

    val all: Flow<List<Transaction>> = dao.getAll()

    suspend fun get(id: Long): Transaction? = dao.getById(id)

    suspend fun add(transaction: Transaction): Long = dao.insert(transaction)

    suspend fun update(transaction: Transaction) = dao.update(transaction)

    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
}
