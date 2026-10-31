package com.finctrl.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finctrl.app.FinctrlApp
import com.finctrl.app.security.SecurityStore
import com.finctrl.app.ui.common.AppBottomBar
import com.finctrl.app.ui.screens.*

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val store = (LocalContext.current.applicationContext as FinctrlApp).container.securityStore
    val onNavigate: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = Routes.LOGIN) {
            composable(Routes.LOGIN) {
                LoginScreen(onReady = {
                    if (nav.previousBackStackEntry != null) nav.popBackStack()
                    else nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
                })
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigate = onNavigate,
                    bottomBar = { AppBottomBar(currentRoute = Routes.DASHBOARD, onNavigate = onNavigate) }
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate(Routes.transactionEdit(it)) },
                    bottomBar = { AppBottomBar(currentRoute = Routes.TRANSACTIONS, onNavigate = onNavigate) }
                )
            }
            composable(Routes.TRANSACTION_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                TransactionEditScreen(onDone = { nav.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onBack = { nav.popBackStack() },
                    bottomBar = { AppBottomBar(currentRoute = Routes.CATEGORIES, onNavigate = onNavigate) }
                )
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    onBack = { nav.popBackStack() },
                    bottomBar = { AppBottomBar(currentRoute = Routes.BUDGETS, onNavigate = onNavigate) }
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen(
                    onBack = { nav.popBackStack() },
                    bottomBar = { AppBottomBar(currentRoute = Routes.REPORTS, onNavigate = onNavigate) }
                )
            }
            composable(Routes.CURRENCIES) {
                CurrenciesScreen(
                    onBack = { nav.popBackStack() },
                    bottomBar = { AppBottomBar(currentRoute = Routes.CURRENCIES, onNavigate = onNavigate) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onLoggedOut = { nav.navigate(Routes.LOGIN) { popUpTo(0) } },
                    bottomBar = { AppBottomBar(currentRoute = Routes.SETTINGS, onNavigate = onNavigate) }
                )
            }
        }
        LockGuard(nav, store)
    }
}

@Composable
private fun LockGuard(nav: NavHostController, store: SecurityStore) {
    val unlocked by store.unlocked.collectAsStateWithLifecycle()
    val pinSet by store.pinSet.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val mustLock = pinSet && !unlocked && route != null && route != Routes.LOGIN

    LaunchedEffect(mustLock) {
        if (mustLock) nav.navigate(Routes.LOGIN) { launchSingleTop = true }
    }
    if (mustLock) Surface(Modifier.fillMaxSize()) {}
}
