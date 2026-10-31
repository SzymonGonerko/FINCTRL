package com.finctrl.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.repo.CurrencyRepository
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CurrenciesState(val loading: Boolean = true, val rates: Map<String, Double> = emptyMap(), val amount: String = "100")

class CurrenciesViewModel(private val repo: CurrencyRepository, errorHandler: ErrorHandler) : BaseViewModel(errorHandler) {
    private val _state = MutableStateFlow(CurrenciesState())
    val state: StateFlow<CurrenciesState> = _state.asStateFlow()

    init { load() }

    fun load(force: Boolean = false) = launchSafely {
        _state.update { it.copy(loading = true) }
        try {
            val rates = repo.rates(force)
            _state.update { it.copy(rates = rates) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    fun setAmount(v: String) = _state.update { it.copy(amount = v) }
}

@Composable
fun CurrenciesScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit = {}, vm: CurrenciesViewModel = appViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val amount = s.amount.replace(',', '.').toDoubleOrNull()
    AppScaffold(
        title = "Waluty", events = vm.events, onBack = onBack, bottomBar = bottomBar,
        actions = { TextButton({ vm.load(force = true) }) { Text("Odśwież") } },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(s.amount, vm::setAmount, label = { Text("Kwota w PLN") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            when {
                s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                s.rates.isEmpty() -> EmptyState("Brak kursów", "Sprawdź połączenie i naciśnij „Odśwież”.")
                else -> LazyColumn {
                    items(s.rates.entries.toList(), key = { it.key }) { (code, rate) ->
                        ListItem(
                            headlineContent = { Text(code) },
                            supportingContent = { Text("1 PLN = ${"%.4f".format(rate)} $code") },
                            trailingContent = { amount?.let { Text("%.2f %s".format(it * rate, code)) } },
                        )
                    }
                }
            }
        }
    }
}
