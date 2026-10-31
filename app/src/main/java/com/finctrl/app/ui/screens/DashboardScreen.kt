package com.finctrl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Money
import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.common.AppScaffold
import com.finctrl.app.ui.common.BaseViewModel
import com.finctrl.app.ui.common.EmptyState
import com.finctrl.app.ui.common.appViewModel
import com.finctrl.app.ui.common.pretty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class DashboardState(
    val month: YearMonth = YearMonth.now(),
    val income: Long = 0,
    val expense: Long = 0,
    val recent: List<TransactionWithCategory> = emptyList(),
) { val balance get() = income - expense }

class DashboardViewModel(repo: FinanceRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    private val month = YearMonth.now()
    val state: StateFlow<DashboardState> = combine(repo.observeTotals(month), repo.observeTransactions()) { totals, txs ->
        DashboardState(
            month = month,
            income = totals.firstOrNull { it.type == TxType.INCOME }?.total ?: 0,
            expense = totals.firstOrNull { it.type == TxType.EXPENSE }?.total ?: 0,
            recent = txs.take(5),
        )
    }.safe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_EDIT = "transaction_edit/{id}"
    fun transactionEdit(id: Long) = "transaction_edit/$id"
    const val CATEGORIES = "categories"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val CURRENCIES = "currencies"
    const val SETTINGS = "settings"
}

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, bottomBar: @Composable () -> Unit = {}, vm: DashboardViewModel = appViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    AppScaffold(
        title = "Pulpit",
        events = vm.events,
        bottomBar = bottomBar,
        actions = { IconButton(onClick = { onNavigate(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, "Ustawienia") } },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigate(Routes.transactionEdit(-1)) }) { Icon(Icons.Default.Add, "Dodaj transakcję") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.month.pretty(), style = MaterialTheme.typography.labelLarge)
                        Text(Money.format(s.balance), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Przychody: ${Money.format(s.income)}")
                        Text("Wydatki: ${Money.format(s.expense)}")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ onNavigate(Routes.TRANSACTIONS) }) { Text("Transakcje") }
                    FilledTonalButton({ onNavigate(Routes.CATEGORIES) }) { Text("Kategorie") }
                    FilledTonalButton({ onNavigate(Routes.BUDGETS) }) { Text("Budżety") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ onNavigate(Routes.REPORTS) }) { Text("Raporty") }
                    FilledTonalButton({ onNavigate(Routes.CURRENCIES) }) { Text("Waluty") }
                }
            }
            item { Text("Ostatnie transakcje", style = MaterialTheme.typography.titleMedium) }
            if (s.recent.isEmpty()) item {
                Box(Modifier.height(160.dp)) { EmptyState("Brak transakcji", "Dodaj pierwszą przyciskiem +") }
            } else items(s.recent, key = { it.transaction.id }) { TransactionRow(it) { onNavigate(Routes.transactionEdit(it.transaction.id)) } }
        }
    }
}
