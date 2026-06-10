package com.fangyang.jizhang.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 一份数据快照：用于同步到云端/从云端恢复。
 * 每次同步都会在云端存一份带时间戳的快照（历史版本）。
 */
data class Snapshot(
    val syncedAt: Long,
    val records: List<ProductRecord>,
    val bindings: List<BarcodeBinding>,
)

/** 快照与 JSON 互转（用 Android 自带的 org.json，无需额外依赖）。 */
object SnapshotCodec {

    private const val VERSION = 1

    fun toJson(snapshot: Snapshot): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("syncedAt", snapshot.syncedAt)

        val records = JSONArray()
        snapshot.records.forEach { r ->
            records.put(
                JSONObject()
                    .put("barcode", r.barcode)
                    .put("name", r.name)
                    .put("purchaseDate", r.purchaseDate)
                    .put("productionDate", r.productionDate)
                    .put("expiryDate", r.expiryDate)
                    .put("purchasePrice", r.purchasePrice)
                    .put("retailPrice", r.retailPrice)
                    .put("recordDate", r.recordDate)
            )
        }
        root.put("records", records)

        val bindings = JSONArray()
        snapshot.bindings.forEach { b ->
            bindings.put(JSONObject().put("barcode", b.barcode).put("name", b.name))
        }
        root.put("bindings", bindings)

        return root.toString()
    }

    fun fromJson(json: String): Snapshot {
        val root = JSONObject(json)
        val syncedAt = root.optLong("syncedAt", 0L)

        val records = mutableListOf<ProductRecord>()
        val recArr = root.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recArr.length()) {
            val o = recArr.getJSONObject(i)
            records.add(
                ProductRecord(
                    id = 0,
                    barcode = o.optString("barcode"),
                    name = o.optString("name"),
                    purchaseDate = o.optLong("purchaseDate"),
                    productionDate = o.optLong("productionDate"),
                    expiryDate = o.optLong("expiryDate"),
                    purchasePrice = o.optDouble("purchasePrice", 0.0),
                    retailPrice = o.optDouble("retailPrice", 0.0),
                    recordDate = o.optLong("recordDate"),
                )
            )
        }

        val bindings = mutableListOf<BarcodeBinding>()
        val bindArr = root.optJSONArray("bindings") ?: JSONArray()
        for (i in 0 until bindArr.length()) {
            val o = bindArr.getJSONObject(i)
            bindings.add(BarcodeBinding(o.optString("barcode"), o.optString("name")))
        }

        return Snapshot(syncedAt, records, bindings)
    }
}

/**
 * 一条记录的内容指纹（不含本地自增 id），用于合并时去重。
 * 同一次扫描在两台设备上 recordDate 相同 → 视为同一条；重新扫的 recordDate 不同 → 保留为新记录。
 */
fun ProductRecord.contentKey(): String =
    listOf(barcode, name, purchaseDate, productionDate, expiryDate, purchasePrice, retailPrice, recordDate)
        .joinToString("|")
