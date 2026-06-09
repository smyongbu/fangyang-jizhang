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
}
