package com.fangyang.jizhang.ui.record

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangyang.jizhang.ui.ProductViewModel
import com.fangyang.jizhang.util.formatYmd

private enum class DateField { PURCHASE, PRODUCTION, EXPIRY }

private val priceRegex = Regex("^\\d*\\.?\\d{0,2}$")
private val LABEL_WIDTH = 84.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormScreen(
    viewModel: ProductViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var datePickerFor by remember { mutableStateOf<DateField?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (form.isEditing) "修改记录" else "填写") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            LabeledRow("条形码") {
                Text(
                    form.barcode.ifBlank { "（无）" },
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }

            LabeledRow("商品名") {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = viewModel::updateName,
                    placeholder = { Text("请输入商品名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (form.nameWasBound) {
                Text(
                    "该条形码已绑定，自动填入",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = LABEL_WIDTH),
                )
            }

            Spacer(Modifier.height(4.dp))
            LabeledRow("进货日期") { DateBox(form.purchaseDate) { datePickerFor = DateField.PURCHASE } }
            LabeledRow("生产日期") { DateBox(form.productionDate) { datePickerFor = DateField.PRODUCTION } }
            LabeledRow("过期日期") { DateBox(form.expiryDate) { datePickerFor = DateField.EXPIRY } }

            LabeledRow("进货价格") {
                PriceField(form.purchasePrice, viewModel::updatePurchasePrice)
            }
            LabeledRow("零售价格") {
                PriceField(form.retailPrice, viewModel::updateRetailPrice)
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = form.canSave,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text("确定", fontSize = 16.sp)
            }
        }
    }

    datePickerFor?.let { field ->
        val (title, current) = when (field) {
            DateField.PURCHASE -> "进货日期" to form.purchaseDate
            DateField.PRODUCTION -> "生产日期" to form.productionDate
            DateField.EXPIRY -> "过期日期" to form.expiryDate
        }
        DatePickerWheelDialog(
            title = title,
            initialMillis = current,
            onDismiss = { datePickerFor = null },
            onSelected = { millis ->
                when (field) {
                    DateField.PURCHASE -> viewModel.updatePurchaseDate(millis)
                    DateField.PRODUCTION -> viewModel.updateProductionDate(millis)
                    DateField.EXPIRY -> viewModel.updateExpiryDate(millis)
                }
                datePickerFor = null
            },
        )
    }
}

/** 左边字段名、右边输入框的一行。 */
@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun DateBox(millis: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatYmd(millis), fontSize = 16.sp)
        Text("选择", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

@Composable
private fun PriceField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || priceRegex.matches(it)) onChange(it) },
        placeholder = { Text("0.00") },
        // 人民币符号一直显示在最右边
        trailingIcon = { Text("¥", fontSize = 16.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
