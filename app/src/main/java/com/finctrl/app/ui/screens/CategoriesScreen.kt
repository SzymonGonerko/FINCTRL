package com.finctrl.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Validators
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private val PALETTE = listOf("#2E7D32", "#00897B", "#1565C0", "#6A1B9A", "#C2185B", "#EF6C00", "#F9A825", "#607D8B")

class CategoriesViewModel(private val repo: FinanceRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    val categories: StateFlow<List<CategoryEntity>> =
        repo.observeCategories().safe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(existing: CategoryEntity?, name: String, type: TxType, colorHex: String) = launchSafely {
        repo.upsertCategory((existing ?: CategoryEntity(name = "", type = type)).copy(
            name = Validators.requireName(name), type = type, colorHex = colorHex))
    }

    fun delete(c: CategoryEntity) = launchSafely { repo.deleteCategory(c.id) }
}

@Composable
fun CategoriesScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit = {}, vm: CategoriesViewModel = appViewModel()) {
    val items by vm.categories.collectAsStateWithLifecycle()
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val editing = editingId?.let { id -> items.firstOrNull { it.id == id } }

    AppScaffold(
        title = "Kategorie", events = vm.events, onBack = onBack, bottomBar = bottomBar,
        floatingActionButton = { FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "Dodaj kategorię") } },
    ) { padding ->
        if (items.isEmpty()) EmptyState("Brak kategorii", "Dodaj pierwszą przyciskiem +")
        else LazyColumn(Modifier.padding(padding)) {
            items(items, key = { it.id }) { c ->
                ListItem(
                    modifier = Modifier.clickable { editingId = c.id },
                    leadingContent = { Box(Modifier.size(20.dp).clip(CircleShape).background(parseColor(c.colorHex))) },
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text(if (c.type == TxType.INCOME) "Przychód" else "Wydatek") },
                    trailingContent = { IconButton({ vm.delete(c) }) { Icon(Icons.Default.Delete, "Usuń") } },
                )
            }
        }
    }
    if (creating || editing != null) {
        CategoryDialog(editing, onDismiss = { creating = false; editingId = null }) { name, type, color ->
            vm.save(editing, name, type, color); creating = false; editingId = null
        }
    }
}

@Composable
private fun CategoryDialog(existing: CategoryEntity?, onDismiss: () -> Unit, onSave: (String, TxType, String) -> Unit) {
    val key = existing?.id
    var name by rememberSaveable(key) { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable(key) { mutableStateOf(existing?.type ?: TxType.EXPENSE) }
    var color by rememberSaveable(key) { mutableStateOf(existing?.colorHex ?: PALETTE.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nowa kategoria" else "Edytuj kategorię") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nazwa") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(type == TxType.EXPENSE, { type = TxType.EXPENSE }, label = { Text("Wydatek") })
                    FilterChip(type == TxType.INCOME, { type = TxType.INCOME }, label = { Text("Przychód") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PALETTE.forEach { hex ->
                        Box(
                            Modifier.size(if (hex == color) 32.dp else 24.dp).clip(CircleShape)
                                .background(parseColor(hex)).clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton({ onSave(name, type, color) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onDismiss) { Text("Anuluj") } },
    )
}
