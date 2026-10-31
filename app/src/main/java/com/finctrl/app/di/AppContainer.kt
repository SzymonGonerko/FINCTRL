package com.finctrl.app.di

import android.content.Context
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.data.local.AppDatabase
import com.finctrl.app.data.repo.AuthRepository
import com.finctrl.app.data.repo.CloudSync
import com.finctrl.app.data.repo.CurrencyRepository
import com.finctrl.app.data.repo.FinanceRepository
import com.finctrl.app.data.repo.RoomFinanceRepository
import com.finctrl.app.security.SecurityStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    val errorHandler = ErrorHandler()
    val securityStore = SecurityStore(context)
    val repository: FinanceRepository = RoomFinanceRepository(AppDatabase.create(context))
    val currencyRepository = CurrencyRepository()
    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }
    val cloudSync by lazy { CloudSync(FirebaseFirestore.getInstance(), authRepository) }
}
