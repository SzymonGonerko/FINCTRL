package com.finctrl.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finctrl.app.R
import com.finctrl.app.core.UiMessage
import com.finctrl.app.data.local.CategoryEntity
import com.finctrl.app.ui.screens.Routes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseColor(hex: String?, fallback: Color = Color(0xFF607D8B)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

private const val MILLIS_PER_DAY = 86_400_000L
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pl"))

private val NavBarBackground = Color(0xFF18181B)
private val NavBarIndicator = Color(0xFF3F3F46)
private val NavBarUnselected = Color(0xFFA1A1AA)

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem(Routes.DASHBOARD, "Pulpit", Icons.Default.Home),
    NavItem(Routes.TRANSACTIONS, "Transakcje", Icons.AutoMirrored.Filled.ReceiptLong),
    NavItem(Routes.CATEGORIES, "Kategorie", Icons.Default.Category),
    NavItem(Routes.BUDGETS, "Budżety", Icons.Default.AccountBalanceWallet),
    NavItem(Routes.REPORTS, "Raporty", Icons.Default.PieChart),
)

fun YearMonth.pretty(): String = format(MONTH_FORMAT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    events: Flow<UiMessage>,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(events) {
        events.collect { msg ->
            val result = snackbar.showSnackbar(
                message = msg.text,
                actionLabel = if (msg.retry != null) "Ponów" else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) msg.retry?.invoke()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo FINCTRL",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(title)
                    }
                },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbar) },
        content = content,
    )
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = NavBarBackground,
        contentColor = Color.White,
        tonalElevation = 8.dp,
    ) {
        NAV_ITEMS.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = NavBarIndicator,
                    unselectedIconColor = NavBarUnselected,
                    unselectedTextColor = NavBarUnselected,
                )
            )
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrev) { Text("‹ Poprzedni") }
        Text(month.pretty(), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onNext) { Text("Następny ›") }
    }
}

@Composable
fun CategoryPicker(categories: List<CategoryEntity>, selectedId: Long?, onSelect: (Long) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text(categories.firstOrNull { it.id == selectedId }?.name ?: "Wybierz kategorię")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            categories.forEach { c ->
                DropdownMenuItem(text = { Text(c.name) }, onClick = { onSelect(c.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(epochDay: Long, onChange: (Long) -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }) { Text(LocalDate.ofEpochDay(epochDay).toString()) }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Math.floorDiv(it, MILLIS_PER_DAY)) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Anuluj") } },
        ) { DatePicker(state = state) }
    }
}
