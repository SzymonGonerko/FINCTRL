package com.finctrl.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Money
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth

data class Slice(val name: String, val color: Color, val total: Long)
data class ReportState(val month: YearMonth, val income: Long = 0, val expense: Long = 0, val slices: List<Slice> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(repo: FinanceRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<ReportState> = month.flatMapLatest { m ->
        combine(repo.observeTotals(m), repo.observeExpenseByCategory(m), repo.observeCategories()) { totals, byCat, cats ->
            val byId = cats.associateBy { it.id }
            ReportState(
                month = m,
                income = totals.firstOrNull { it.type == TxType.INCOME }?.total ?: 0,
                expense = totals.firstOrNull { it.type == TxType.EXPENSE }?.total ?: 0,
                slices = byCat.sortedByDescending { it.total }.map {
                    val c = byId[it.categoryId]
                    Slice(c?.name ?: "Bez kategorii", parseColor(c?.colorHex), it.total)
                },
            )
        }
    }.safe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState(YearMonth.now()))

    fun prev() = month.update { it.minusMonths(1) }
    fun next() = month.update { it.plusMonths(1) }
}

@Composable
fun ReportsScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit = {}, vm: ReportsViewModel = appViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    AppScaffold(title = "Raporty", events = vm.events, onBack = onBack, bottomBar = bottomBar) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthSwitcher(s.month, vm::prev, vm::next)
            Text("Przychody: ${Money.format(s.income)}")
            Text("Wydatki: ${Money.format(s.expense)}")
            Text("Bilans: ${Money.format(s.income - s.expense)}", style = MaterialTheme.typography.titleMedium)
            if (s.slices.isEmpty()) EmptyState("Brak wydatków", "W tym miesiącu nie ma danych do wykresu.")
            else {
                Text("Wydatki wg kategorii", style = MaterialTheme.typography.titleMedium)
                PieChart(s.slices, Modifier.size(200.dp).align(androidx.compose.ui.Alignment.CenterHorizontally))
                s.slices.forEach { sl ->
                    val pct = if (s.expense > 0) sl.total * 100 / s.expense else 0
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(14.dp).background(sl.color))
                        Text("${sl.name}: ${Money.format(sl.total)} ($pct%)")
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(slices: List<Slice>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val total = slices.sumOf { it.total }.toFloat().takeIf { it > 0 } ?: return@Canvas
        var start = -90f
        slices.forEach { sl ->
            val sweep = 360f * sl.total / total
            drawArc(sl.color, start, sweep, useCenter = true)
            start += sweep
        }
    }
}
