package com.fangyang.jizhang.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /** 查询页：按记录时间升序，最新的在最下面。 */
    @Query("SELECT * FROM product_records ORDER BY recordDate ASC")
    fun getAllRecords(): Flow<List<ProductRecord>>

    @Insert
    suspend fun insertRecord(record: ProductRecord): Long

    /** 按条形码查绑定的商品名，没绑定返回 null。 */
    @Query("SELECT * FROM barcode_bindings WHERE barcode = :barcode LIMIT 1")
    suspend fun findBinding(barcode: String): BarcodeBinding?

    /** 绑定/更新条形码对应的商品名（一个条形码只保留一个名字）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBinding(binding: BarcodeBinding)
}
