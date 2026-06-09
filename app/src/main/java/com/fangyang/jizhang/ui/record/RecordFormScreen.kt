package com.fangyang.jizhang.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangyang.jizhang.ui.ProductViewModel
import com.fangyang.jizhang.util.formatYmd

private enum class DateField { PURCHASE, PRODUCTION, EXPIRY }

private val priceRegex = Regex("^\\d*\\.?\\d{0,2}$")

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
                title = { Text("填写") },
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
            // 条形码
            Text("条形码", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                form.barcode.ifBlank { "（无）" },
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            // 商品名
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::updateName,
                label = { Text("商品名") },
                supportingText = if (form.nameWasBound) {
                    { Text("该条形码已绑定，自动填入") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // 日期
            DateRow("进货日期", form.purchaseDate) { datePickerFor = DateField.PURCHASE }
            Spacer(Modifier.height(10.dp))
            DateRow("生产日期", form.productionDate) { datePickerFor = DateField.PRODUCTION }
            Spacer(Modifier.height(10.dp))
            DateRow("过期日期", form.expiryDate) { datePickerFor = DateField.EXPIRY }
            Spacer(Modifier.height(16.dp))

            // 价格
            PriceField("进货价格", form.purchasePrice, viewModel::updatePurchasePrice)
            Spacer(Modifier.height(12.dp))
            PriceField("零售价格", form.retailPrice, viewModel::updateRetailPrice)
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
        CascadingDatePickerDialog(
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

@Composable
private fun DateRow(label: String, millis: Long, onClick: () -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatYmd(millis), fontSize = 16.sp)
            Text("点击选择", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PriceField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || priceRegex.matches(it)) onChange(it) },
        label = { Text(label) },
        suffix = { Text("¥") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
