package com.finctrl.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class TransactionsViewModel(repo: FinanceRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    val query = MutableStateFlow("")
    val typeFilter = MutableStateFlow<TxType?>(null)

    val items: StateFlow<List<TransactionWithCategory>> =
        combine(repo.observeTransactions(), query, typeFilter) { all, q, type ->
            all.filter { i ->
                (type == null || i.transaction.type == type) &&
                    (q.isBlank() || i.transaction.note.contains(q, true) || (i.categoryName?.contains(q, true) ?: false))
            }
        }.safe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun TransactionsScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, bottomBar: @Composable () -> Unit = {}, vm: TransactionsViewModel = appViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val type by vm.typeFilter.collectAsStateWithLifecycle()
    AppScaffold(
        title = "Transakcje",
        events = vm.events,
        onBack = onBack,
        bottomBar = bottomBar,
        floatingActionButton = { FloatingActionButton(onClick = { onOpen(-1) }) { Icon(Icons.Default.Add, "Dodaj") } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(query, { vm.query.value = it }, label = { Text("Szukaj (notatka, kategoria)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                FilterChip(type == null, { vm.typeFilter.value = null }, label = { Text("Wszystkie") })
                FilterChip(type == TxType.INCOME, { vm.typeFilter.value = TxType.INCOME }, label = { Text("Przychody") })
                FilterChip(type == TxType.EXPENSE, { vm.typeFilter.value = TxType.EXPENSE }, label = { Text("Wydatki") })
            }
            if (items.isEmpty()) EmptyState("Brak wyników", "Zmień filtry lub dodaj transakcję.")
            else LazyColumn { items(items, key = { it.transaction.id }) { TransactionRow(it) { onOpen(it.transaction.id) } } }
        }
    }
}

@Composable
fun TransactionRow(item: TransactionWithCategory, onClick: () -> Unit) {
    val t = item.transaction
    val sign = if (t.type == TxType.INCOME) "+" else "−"
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.categoryName ?: "Bez kategorii") },
        supportingContent = { Text("${LocalDate.ofEpochDay(t.date)}  ${t.note}".trim()) },
        trailingContent = {
            Text(
                "$sign${Money.format(t.amount, t.currency)}",
                fontWeight = FontWeight.SemiBold,
                color = if (t.type == TxType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        },
    )
}
