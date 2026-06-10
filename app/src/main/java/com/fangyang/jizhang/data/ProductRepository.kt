package com.fangyang.jizhang.data

import kotlinx.coroutines.flow.Flow

/**
 * 数据仓库。界面层只跟它打交道。
 * 以后要加云同步，在这一层接入即可。
 */
class ProductRepository(private val dao: ProductDao) {

    val allRecords: Flow<List<ProductRecord>> = dao.getAllRecords()

    val allBindings: Flow<List<BarcodeBinding>> = dao.getAllBindings()

    /** 改名：同时更新绑定关系和该条形码下已有记录。 */
    suspend fun renameProduct(barcode: String, newName: String) {
        val name = newName.trim()
        dao.updateBindingName(barcode, name)
        dao.updateRecordsName(barcode, name)
    }

    /** 条形码对应的商品名，没绑定返回 null。 */
    suspend fun nameForBarcode(barcode: String): String? =
        dao.findBinding(barcode)?.name

    /** 保存一条进货记录，并记住「条形码 → 商品名」的绑定。 */
    suspend fun saveRecord(record: ProductRecord) {
        dao.insertRecord(record)
        dao.upsertBinding(BarcodeBinding(record.barcode, record.name))
    }

    /** 修改一条已有记录。 */
    suspend fun updateRecord(record: ProductRecord) {
        dao.updateRecord(record)
    }

    // ---- 同步：导出快照 / 合并 / 覆盖 ----

    /** 把本地所有数据打包成快照。 */
    suspend fun exportSnapshot(): Snapshot =
        Snapshot(
            syncedAt = System.currentTimeMillis(),
            records = dao.getAllRecordsOnce(),
            bindings = dao.getAllBindingsOnce(),
        )

    /** 本地是否有任何数据。 */
    suspend fun isLocalEmpty(): Boolean =
        dao.getAllRecordsOnce().isEmpty() && dao.getAllBindingsOnce().isEmpty()

    /** 合并：把快照里本地没有的记录/绑定加进来（按内容指纹去重，冲突时保留本地绑定）。 */
    suspend fun mergeWith(snapshot: Snapshot) {
        val localKeys = dao.getAllRecordsOnce().map { it.contentKey() }.toSet()
        val newRecords = snapshot.records
            .filter { it.contentKey() !in localKeys }
            .map { it.copy(id = 0) }
        if (newRecords.isNotEmpty()) dao.insertRecords(newRecords)

        val localBarcodes = dao.getAllBindingsOnce().map { it.barcode }.toSet()
        val newBindings = snapshot.bindings.filter { it.barcode !in localBarcodes }
        if (newBindings.isNotEmpty()) dao.insertBindings(newBindings)
    }

    /** 用快照覆盖本地（保留云端数据）。 */
    suspend fun replaceWith(snapshot: Snapshot) {
        dao.clearRecords()
        dao.clearBindings()
        dao.insertRecords(snapshot.records.map { it.copy(id = 0) })
        dao.insertBindings(snapshot.bindings)
    }
}
