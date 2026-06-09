package com.fangyang.jizhang.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fangyang.jizhang.FangYangApp
import com.fangyang.jizhang.data.Transaction
import com.fangyang.jizhang.data.TransactionRepository
import com.fangyang.jizhang.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 首页一天的账单分组。 */
data class DayGroup(
    val date: LocalDate,
    val items: List<Transaction>,
    val dayBalance: Double,   // 当天净额（收入 - 支出）
)

/** 首页 UI 状态。 */
data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val groups: List<DayGroup> = emptyList(),
)

class TransactionViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    val uiState: StateFlow<HomeUiState> =
        combine(repository.all, _month) { all, month ->
            val monthItems = all.filter { yearMonthOf(it.date) == month }

            val income = monthItems
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val expense = monthItems
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val groups = monthItems
                .groupBy { localDateOf(it.date) }
                .toSortedMap(reverseOrder())
                .map { (date, items) ->
                    val dayBalance = items.sumOf {
                        if (it.type == TransactionType.EXPENSE) -it.amount else it.amount
                    }
                    DayGroup(date, items.sortedByDescending { it.date }, dayBalance)
                }

            HomeUiState(month, income, expense, income - expense, groups)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun prevMonth() {
        _month.value = _month.value.minusMonths(1)
    }

    fun nextMonth() {
        _month.value = _month.value.plusMonths(1)
    }

    suspend fun getTransaction(id: Long): Transaction? = repository.get(id)

    /** id == 0 表示新增，否则是编辑。 */
    fun save(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: String,
        note: String,
        date: Long,
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                id = id,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = date,
            )
            if (id == 0L) repository.add(tx) else repository.update(tx)
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.delete(transaction) }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.get(id)?.let { repository.delete(it) }
        }
    }

    private fun localDateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun yearMonthOf(epochMillis: Long): YearMonth =
        YearMonth.from(localDateOf(epochMillis))

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FangYangApp
                TransactionViewModel(app.repository)
            }
        }
    }
}
