package com.finctrl.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finctrl.app.R
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.Validators
import com.finctrl.app.data.repo.AuthRepository
import com.finctrl.app.security.BiometricHelper
import com.finctrl.app.security.SecurityStore
import com.finctrl.app.ui.common.AppScaffold
import com.finctrl.app.ui.common.BaseViewModel
import com.finctrl.app.ui.common.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LoginMode { AUTH, UNLOCK, READY }
data class LoginUiState(val mode: LoginMode, val busy: Boolean = false)

class LoginViewModel(
    private val auth: AuthRepository,
    private val store: SecurityStore,
    errorHandler: ErrorHandler,
) : BaseViewModel(errorHandler) {

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    val biometricEnabled: Boolean get() = store.biometricEnabled

    private fun compute(busy: Boolean = false): LoginUiState {
        val authed = store.localMode || auth.isSignedIn
        val mode = when {
            !authed -> LoginMode.AUTH
            store.isPinSet && !store.unlocked.value -> LoginMode.UNLOCK
            else -> LoginMode.READY
        }
        return LoginUiState(mode, busy)
    }

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    fun setPassword(v: String) { _password.value = v }
    fun setPin(v: String) { if (v.length <= 6 && v.all(Char::isDigit)) _pin.value = v }

    fun signIn(email: String) = authenticate(email, _password.value, register = false)
    fun register(email: String) = authenticate(email, _password.value, register = true)

    private fun authenticate(email: String, password: String, register: Boolean) = launchSafely {
        val e = Validators.validateEmail(email)
        val p = Validators.validatePassword(password)
        _state.update { it.copy(busy = true) }
        try {
            if (register) auth.register(e, p) else auth.signIn(e, p)
        } finally {
            _state.value = compute()
        }
    }

    fun continueLocally() { store.localMode = true; _state.value = compute() }

    fun unlockWithPin() {
        val ok = store.verifyPin(_pin.value)
        _pin.value = ""
        if (ok) _state.value = compute() else notify("Nieprawidłowy PIN.")
    }

    fun onBiometricSuccess() { store.markUnlocked(); _state.value = compute() }
}

@Composable
fun LoginScreen(onReady: () -> Unit, vm: LoginViewModel = appViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val pin by vm.pin.collectAsStateWithLifecycle()
    val activity = LocalContext.current as FragmentActivity

    BackHandler(enabled = state.mode == LoginMode.UNLOCK) { activity.moveTaskToBack(true) }

    LaunchedEffect(state.mode) { if (state.mode == LoginMode.READY) onReady() }
    LaunchedEffect(state.mode) {
        if (state.mode == LoginMode.UNLOCK && vm.biometricEnabled && BiometricHelper.isAvailable(activity)) {
            BiometricHelper.prompt(activity, vm::onBiometricSuccess, onError = { })
        }
    }

    AppScaffold(title = "FINCTRL", events = vm.events) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo FINCTRL",
                modifier = Modifier
                    .size(130.dp)
                    .padding(bottom = 20.dp)
            )
            when (state.mode) {
                LoginMode.AUTH -> AuthForm(
                    state.busy, password, vm::setPassword, vm::signIn, vm::register, vm::continueLocally,
                )
                LoginMode.UNLOCK -> UnlockForm(
                    pin, vm::setPin, vm::unlockWithPin,
                    showBiometric = vm.biometricEnabled && BiometricHelper.isAvailable(activity),
                    onBiometric = { BiometricHelper.prompt(activity, vm::onBiometricSuccess, onError = { vm.notify(it) }) },
                )
                LoginMode.READY -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun AuthForm(
    busy: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    onSignIn: (String) -> Unit,
    onRegister: (String) -> Unit,
    onLocal: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    Text("Zaloguj się", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(password, onPasswordChange, label = { Text("Hasło") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Button(onClick = { onSignIn(email) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Zaloguj") }
    OutlinedButton(onClick = { onRegister(email) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Utwórz konto") }
    TextButton(onClick = onLocal) { Text("Kontynuuj bez konta (tryb lokalny)") }
}

@Composable
private fun UnlockForm(pin: String, onPinChange: (String) -> Unit, onSubmit: () -> Unit, showBiometric: Boolean, onBiometric: () -> Unit) {
    Text("Wpisz PIN", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(pin, onPinChange, label = { Text("PIN") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    Button(onClick = onSubmit, enabled = pin.length >= 4, modifier = Modifier.fillMaxWidth()) { Text("Odblokuj") }
    if (showBiometric) OutlinedButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Text("Użyj biometrii") }
}
