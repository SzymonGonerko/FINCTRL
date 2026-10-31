package com.finctrl.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.finctrl.app.di.AppContainer
import com.finctrl.app.security.AutoLockObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FinctrlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AutoLockObserver(container.securityStore))
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { container.repository.seedDefaultsIfEmpty() }
    }
}
