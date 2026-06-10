package com.fangyang.jizhang.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    /** 查询页：按记录时间升序，最新的在最下面。 */
    @Query("SELECT * FROM product_records ORDER BY recordDate ASC")
    fun getAllRecords(): Flow<List<ProductRecord>>

    @Insert
    suspend fun insertRecord(record: ProductRecord): Long

    @Update
    suspend fun updateRecord(record: ProductRecord)

    /** 按条形码查绑定的商品名，没绑定返回 null。 */
    @Query("SELECT * FROM barcode_bindings WHERE barcode = :barcode LIMIT 1")
    suspend fun findBinding(barcode: String): BarcodeBinding?

    /** 该条形码最近一次的记录（用于带出上次的价格）。 */
    @Query("SELECT * FROM product_records WHERE barcode = :barcode ORDER BY recordDate DESC LIMIT 1")
    suspend fun lastRecordForBarcode(barcode: String): ProductRecord?

    /** 绑定/更新条形码对应的商品名（一个条形码只保留一个名字）。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBinding(binding: BarcodeBinding)

    /** 设置页：所有已绑定的条形码。 */
    @Query("SELECT * FROM barcode_bindings ORDER BY name ASC")
    fun getAllBindings(): Flow<List<BarcodeBinding>>

    @Query("UPDATE barcode_bindings SET name = :name WHERE barcode = :barcode")
    suspend fun updateBindingName(barcode: String, name: String)

    /** 同步更新该条形码已有记录里的商品名。 */
    @Query("UPDATE product_records SET name = :name WHERE barcode = :barcode")
    suspend fun updateRecordsName(barcode: String, name: String)

    // ---- 同步用：一次性读取 / 批量写入 / 清空 ----

    @Query("SELECT * FROM product_records")
    suspend fun getAllRecordsOnce(): List<ProductRecord>

    @Query("SELECT * FROM barcode_bindings")
    suspend fun getAllBindingsOnce(): List<BarcodeBinding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<ProductRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBindings(bindings: List<BarcodeBinding>)

    @Query("DELETE FROM product_records")
    suspend fun clearRecords()

    @Query("DELETE FROM barcode_bindings")
    suspend fun clearBindings()
}
