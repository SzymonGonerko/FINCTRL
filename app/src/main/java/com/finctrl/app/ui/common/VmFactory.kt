package com.finctrl.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finctrl.app.FinctrlApp
import com.finctrl.app.di.AppContainer
import com.finctrl.app.ui.screens.*

class VmFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when (modelClass) {
        LoginViewModel::class.java -> LoginViewModel(c.authRepository, c.securityStore, c.errorHandler)
        DashboardViewModel::class.java -> DashboardViewModel(c.repository, c.errorHandler)
        TransactionsViewModel::class.java -> TransactionsViewModel(c.repository, c.errorHandler)
        TransactionEditViewModel::class.java ->
            TransactionEditViewModel(extras.createSavedStateHandle(), c.repository, c.errorHandler)
        CategoriesViewModel::class.java -> CategoriesViewModel(c.repository, c.errorHandler)
        BudgetsViewModel::class.java -> BudgetsViewModel(c.repository, c.errorHandler)
        ReportsViewModel::class.java -> ReportsViewModel(c.repository, c.errorHandler)
        CurrenciesViewModel::class.java -> CurrenciesViewModel(c.currencyRepository, c.errorHandler)
        SettingsViewModel::class.java ->
            SettingsViewModel(c.repository, c.authRepository, c.cloudSync, c.securityStore, c.errorHandler)
        else -> error("Nieznany ViewModel: ${modelClass.name}")
    } as T
}

@Composable
inline fun <reified VM : ViewModel> appViewModel(): VM {
    val container = (LocalContext.current.applicationContext as FinctrlApp).container
    return viewModel(factory = VmFactory(container))
}
