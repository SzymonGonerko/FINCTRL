package com.finctrl.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.ValidationException
import com.finctrl.app.core.Validators
import com.finctrl.app.data.repo.AuthRepository
import com.finctrl.app.data.repo.CloudSync
import com.finctrl.app.data.repo.CsvExporter
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.security.BiometricHelper
import com.finctrl.app.security.SecurityStore
import com.finctrl.app.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

data class SettingsState(
    val pinSet: Boolean, val biometricEnabled: Boolean, val signedIn: Boolean, val email: String?, val busy: Boolean = false,
)

class SettingsViewModel(
    private val repo: FinanceRepository,
    private val auth: AuthRepository,
    private val cloud: CloudSync,
    private val store: SecurityStore,
    errorHandler: ErrorHandler,
) : BaseViewModel(errorHandler) {

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun read(busy: Boolean = false) = SettingsState(
        pinSet = store.isPinSet, biometricEnabled = store.biometricEnabled,
        signedIn = !store.localMode && auth.isSignedIn, email = if (store.localMode) null else auth.email, busy = busy,
    )

    private val _newPin = MutableStateFlow("")
    val newPin: StateFlow<String> = _newPin.asStateFlow()
    fun setNewPin(v: String) { if (v.length <= 6 && v.all(Char::isDigit)) _newPin.value = v }
    fun clearNewPin() { _newPin.value = "" }

    fun savePin() = launchSafely {
        store.setPin(Validators.validatePin(_newPin.value))
        clearNewPin(); _state.value = read(); notify("PIN zapisany.")
    }
    fun removePin() { store.clearPin(); _state.value = read() }
    fun setBiometric(on: Boolean) { store.biometricEnabled = on && store.isPinSet; _state.value = read() }

    fun backup() = cloudJob("Kopia zapasowa zapisana w chmurze.") { cloud.backup(repo.snapshot()) }
    fun restore() = cloudJob("Dane przywrócone z chmury.") { repo.replaceAll(cloud.restore()) }

    private fun cloudJob(done: String, block: suspend () -> Unit) = launchSafely {
        if (!state.value.signedIn) throw ValidationException("Kopie zapasowe wymagają zalogowanego konta (nie działa w trybie lokalnym).")
        _state.value = read(busy = true)
        try { block(); notify(done) } finally { _state.value = read() }
    }

    fun exportCsv(open: () -> OutputStream?) = launchSafely {
        val csv = CsvExporter.toCsv(repo.observeTransactions().first())
        withContext(Dispatchers.IO) {
            (open() ?: throw IOException("Nie można otworzyć pliku do zapisu.")).use { it.write(csv.toByteArray(Charsets.UTF_8)) }
        }
        notify("Wyeksportowano do CSV.")
    }

    fun signOut() { store.lock(); store.localMode = false; if (auth.isSignedIn) auth.signOut() }
    fun lockNow() = store.lock()
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onLoggedOut: () -> Unit, bottomBar: @Composable () -> Unit = {}, vm: SettingsViewModel = appViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val newPin by vm.newPin.collectAsStateWithLifecycle()
    var pinDialog by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv { context.contentResolver.openOutputStream(uri) }
    }

    AppScaffold(title = "Ustawienia", events = vm.events, onBack = onBack, bottomBar = bottomBar) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Konto: ${s.email ?: "tryb lokalny"}")

            Text("Bezpieczeństwo", style = MaterialTheme.typography.titleMedium)
            Button({ pinDialog = true }, Modifier.fillMaxWidth()) { Text(if (s.pinSet) "Zmień PIN" else "Ustaw PIN") }
            if (s.pinSet) OutlinedButton(vm::removePin, Modifier.fillMaxWidth()) { Text("Usuń PIN") }
            if (s.pinSet && BiometricHelper.isAvailable(context)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Odblokowanie biometrią")
                    Switch(s.biometricEnabled, vm::setBiometric)
                }
            }

            Text("Dane", style = MaterialTheme.typography.titleMedium)
            Button({ exporter.launch("finctrl-transakcje.csv") }, Modifier.fillMaxWidth()) { Text("Eksportuj do CSV") }
            Button(vm::backup, Modifier.fillMaxWidth(), enabled = !s.busy) { Text("Kopia zapasowa do chmury") }
            OutlinedButton({ confirmRestore = true }, Modifier.fillMaxWidth(), enabled = !s.busy) { Text("Przywróć z chmury") }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            HorizontalDivider()
            OutlinedButton({ vm.signOut(); onLoggedOut() }, Modifier.fillMaxWidth()) { Text("Wyloguj / zmień tryb") }
        }
    }

    if (pinDialog) {
        val closePinDialog = { vm.clearNewPin(); pinDialog = false }
        AlertDialog(
            onDismissRequest = closePinDialog,
            title = { Text("Nowy PIN (4–6 cyfr)") },
            text = {
                OutlinedTextField(newPin, vm::setNewPin, singleLine = true, label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            },
            confirmButton = { TextButton({ vm.savePin(); pinDialog = false }) { Text("Zapisz") } },
            dismissButton = { TextButton(closePinDialog) { Text("Anuluj") } },
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Przywrócić dane z chmury?") },
            text = { Text("Lokalne dane zostaną zastąpione kopią z chmury.") },
            confirmButton = { TextButton({ vm.restore(); confirmRestore = false }) { Text("Przywróć") } },
            dismissButton = { TextButton({ confirmRestore = false }) { Text("Anuluj") } },
        )
    }
}
