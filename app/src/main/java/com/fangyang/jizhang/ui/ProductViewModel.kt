package com.fangyang.jizhang.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fangyang.jizhang.FangYangApp
import com.fangyang.jizhang.data.BarcodeBinding
import com.fangyang.jizhang.data.ProductRecord
import com.fangyang.jizhang.data.ProductRepository
import com.fangyang.jizhang.util.formatAmount
import com.fangyang.jizhang.util.todayStartMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 填写界面的表单状态。 */
data class FormState(
    val barcode: String = "",
    val name: String = "",
    val nameWasBound: Boolean = false,  // 该条形码之前是否已绑定过名字
    val purchaseDate: Long = todayStartMillis(),
    val productionDate: Long = todayStartMillis(),
    val expiryDate: Long = todayStartMillis(),
    val purchasePrice: String = "",
    val retailPrice: String = "",
    val editingId: Long? = null,        // 非空表示在修改已有记录
    val originalRecordDate: Long = 0L,  // 修改时保留原记录时间
) {
    val isEditing: Boolean get() = editingId != null

    val canSave: Boolean
        get() = name.isNotBlank() &&
            purchasePrice.toDoubleOrNull() != null &&
            retailPrice.toDoubleOrNull() != null
}

class ProductViewModel(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    val records: StateFlow<List<ProductRecord>> =
        repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val bindings: StateFlow<List<BarcodeBinding>> =
        repository.allBindings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 设置页：修改条形码绑定的商品名。 */
    fun renameProduct(barcode: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameProduct(barcode, newName) }
    }

    /** 扫到条形码后初始化表单；若已绑定过名字则自动填入。 */
    fun startForm(barcode: String) {
        _form.value = FormState(barcode = barcode)
        viewModelScope.launch {
            val name = repository.nameForBarcode(barcode)
            if (name != null) {
                _form.update { it.copy(name = name, nameWasBound = true) }
            }
        }
    }

    /** 从查询页点进来修改已有记录，填充表单。 */
    fun startEdit(record: ProductRecord) {
        _form.value = FormState(
            barcode = record.barcode,
            name = record.name,
            nameWasBound = false,
            purchaseDate = record.purchaseDate,
            productionDate = record.productionDate,
            expiryDate = record.expiryDate,
            purchasePrice = formatAmount(record.purchasePrice),
            retailPrice = formatAmount(record.retailPrice),
            editingId = record.id,
            originalRecordDate = record.recordDate,
        )
    }

    fun updateName(v: String) = _form.update { it.copy(name = v) }
    fun updatePurchaseDate(v: Long) = _form.update { it.copy(purchaseDate = v) }
    fun updateProductionDate(v: Long) = _form.update { it.copy(productionDate = v) }
    fun updateExpiryDate(v: Long) = _form.update { it.copy(expiryDate = v) }
    fun updatePurchasePrice(v: String) = _form.update { it.copy(purchasePrice = v) }
    fun updateRetailPrice(v: String) = _form.update { it.copy(retailPrice = v) }

    /** 保存记录并绑定条形码-名字，完成后清空表单并回调。 */
    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (!f.canSave) return
        viewModelScope.launch {
            val record = ProductRecord(
                id = f.editingId ?: 0L,
                barcode = f.barcode,
                name = f.name.trim(),
                purchaseDate = f.purchaseDate,
                productionDate = f.productionDate,
                expiryDate = f.expiryDate,
                purchasePrice = f.purchasePrice.toDouble(),
                retailPrice = f.retailPrice.toDouble(),
                recordDate = if (f.isEditing) f.originalRecordDate else System.currentTimeMillis(),
            )
            if (f.isEditing) repository.updateRecord(record) else repository.saveRecord(record)
            _form.value = FormState()
            onSaved()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FangYangApp
                ProductViewModel(app.repository)
            }
        }
    }
}
