package com.finctrl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Money
import com.finctrl.app.core.ValidationException
import com.finctrl.app.core.Validators
import com.finctrl.app.data.local.BudgetEntity
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth

data class BudgetItem(val budget: BudgetEntity, val categoryName: String, val spent: Long) {
    val progress get() = if (budget.limitAmount == 0L) 0f else (spent.toFloat() / budget.limitAmount).coerceIn(0f, 1f)
    val exceeded get() = spent > budget.limitAmount
}

data class BudgetsState(val month: YearMonth, val items: List<BudgetItem> = emptyList(), val expenseCategories: List<CategoryEntity> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(private val repo: FinanceRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<BudgetsState> = month.flatMapLatest { m ->
        combine(repo.observeBudgets(m), repo.observeExpenseByCategory(m), repo.observeCategories()) { budgets, spent, cats ->
            val byId = cats.associateBy { it.id }
            BudgetsState(
                month = m,
                items = budgets.map { b ->
                    BudgetItem(b, byId[b.categoryId]?.name ?: "?", spent.firstOrNull { it.categoryId == b.categoryId }?.total ?: 0)
                },
                expenseCategories = cats.filter { it.type == TxType.EXPENSE },
            )
        }
    }.safe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsState(YearMonth.now()))

    fun prev() = month.update { it.minusMonths(1) }
    fun next() = month.update { it.plusMonths(1) }

    fun save(categoryId: Long?, limitRaw: String) = launchSafely {
        val cat = categoryId ?: throw ValidationException("Wybierz kategorię.")
        repo.upsertBudget(BudgetEntity(
            id = state.value.items.firstOrNull { it.budget.categoryId == cat }?.budget?.id ?: 0,
            categoryId = cat, month = month.value.toString(), limitAmount = Validators.parseAmount(limitRaw)))
    }

    fun delete(b: BudgetEntity) = launchSafely { repo.deleteBudget(b.id) }
}

@Composable
fun BudgetsScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit = {}, vm: BudgetsViewModel = appViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf(false) }
    AppScaffold(
        title = "Budżety", events = vm.events, onBack = onBack, bottomBar = bottomBar,
        floatingActionButton = { FloatingActionButton(onClick = { dialog = true }) { Icon(Icons.Default.Add, "Dodaj budżet") } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            MonthSwitcher(s.month, vm::prev, vm::next)
            if (s.items.isEmpty()) EmptyState("Brak budżetów", "Ustaw limit dla kategorii wydatków przyciskiem +")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.items, key = { it.budget.id }) { i ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(i.categoryName, style = MaterialTheme.typography.titleMedium)
                                IconButton({ vm.delete(i.budget) }) { Icon(Icons.Default.Delete, "Usuń budżet") }
                            }
                            LinearProgressIndicator(
                                progress = { i.progress }, modifier = Modifier.fillMaxWidth(),
                                color = if (i.exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                            Text("${Money.format(i.spent)} z ${Money.format(i.budget.limitAmount)}" + if (i.exceeded) " – przekroczono!" else "")
                        }
                    }
                }
            }
        }
    }
    if (dialog) {
        var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
        var limit by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Limit na ${s.month.pretty()}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPicker(s.expenseCategories, categoryId) { categoryId = it }
                    OutlinedTextField(limit, { limit = it }, label = { Text("Limit (PLN)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = { TextButton({ vm.save(categoryId, limit); dialog = false }) { Text("Zapisz") } },
            dismissButton = { TextButton({ dialog = false }) { Text("Anuluj") } },
        )
    }
}
