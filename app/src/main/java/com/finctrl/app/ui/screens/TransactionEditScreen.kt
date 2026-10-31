package com.finctrl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Validators
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class EditForm(
    val type: TxType = TxType.EXPENSE,
    val amount: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val date: Long = LocalDate.now().toEpochDay(),
    val currency: String = "PLN",
    val amountError: String? = null,
    val categoryError: String? = null,
    val isEdit: Boolean = false,
)

class TransactionEditViewModel(
    handle: SavedStateHandle,
    private val repo: FinanceRepository,
    errorHandler: ErrorHandler,
) : BaseViewModel(errorHandler) {

    private val id: Long = handle.get<Long>("id") ?: -1L
    private val _form = MutableStateFlow(EditForm(isEdit = id > 0))
    val form: StateFlow<EditForm> = _form.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        combine(repo.observeCategories(), _form.map { it.type }.distinctUntilChanged()) { all, type -> all.filter { it.type == type } }
            .safe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (id > 0) launchSafely {
            repo.getTransaction(id)?.let { t ->
                _form.value = EditForm(
                    type = t.type, amount = "%d.%02d".format(t.amount / 100, t.amount % 100),
                    categoryId = t.categoryId, note = t.note, date = t.date, currency = t.currency, isEdit = true,
                )
            }
        }
    }

    fun setType(type: TxType) = _form.update { it.copy(type = type, categoryId = null, categoryError = null) }
    fun setAmount(v: String) = _form.update { it.copy(amount = v, amountError = null) }
    fun setCategory(id: Long) = _form.update { it.copy(categoryId = id, categoryError = null) }
    fun setNote(v: String) = _form.update { it.copy(note = v.take(120)) }
    fun setDate(epochDay: Long) = _form.update { it.copy(date = epochDay) }

    fun save() {
        val f = _form.value
        val amount = runCatching { Validators.parseAmount(f.amount) }
        val categoryError = if (f.categoryId == null) "Wybierz kategorię." else null
        _form.update { it.copy(amountError = amount.exceptionOrNull()?.message, categoryError = categoryError) }
        if (amount.isFailure || categoryError != null) return
        launchSafely {
            repo.upsertTransaction(
                TransactionEntity(
                    id = if (id > 0) id else 0, amount = amount.getOrThrow(), type = f.type,
                    categoryId = f.categoryId, note = f.note.trim(), date = f.date, currency = f.currency,
                )
            )
            _saved.send(Unit)
        }
    }

    fun delete() {
        if (id <= 0) return
        launchSafely { repo.deleteTransaction(id); _saved.send(Unit) }
    }
}

object TxEditTags {
    const val AMOUNT = "tx_amount"
    const val SAVE = "tx_save"
}

@Composable
fun TransactionEditScreen(onDone: () -> Unit, vm: TransactionEditViewModel = appViewModel()) {
    val f by vm.form.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.saved.collect { onDone() } }

    AppScaffold(
        title = if (f.isEdit) "Edytuj transakcję" else "Nowa transakcja",
        events = vm.events,
        onBack = onDone,
        actions = { if (f.isEdit) IconButton(onClick = vm::delete) { Icon(Icons.Default.Delete, "Usuń") } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(f.type == TxType.EXPENSE, { vm.setType(TxType.EXPENSE) }, label = { Text("Wydatek") })
                FilterChip(f.type == TxType.INCOME, { vm.setType(TxType.INCOME) }, label = { Text("Przychód") })
            }
            OutlinedTextField(
                f.amount, vm::setAmount, label = { Text("Kwota (${f.currency})") }, singleLine = true,
                isError = f.amountError != null, supportingText = { f.amountError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag(TxEditTags.AMOUNT),
            )
            CategoryPicker(categories, f.categoryId, vm::setCategory)
            f.categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            DatePickerField(f.date, vm::setDate)
            OutlinedTextField(f.note, vm::setNote, label = { Text("Notatka") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().testTag(TxEditTags.SAVE)) { Text("Zapisz") }
        }
    }
}
