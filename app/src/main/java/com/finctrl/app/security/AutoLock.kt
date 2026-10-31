package com.finctrl.app.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AutoLockPolicy(private val timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
    private var backgroundSince: Long? = null

    fun onBackground(nowMs: Long) { backgroundSince = nowMs }

    fun onForeground(nowMs: Long): Boolean {
        val since = backgroundSince ?: return false
        backgroundSince = null
        return nowMs - since >= timeoutMs
    }

    companion object { const val DEFAULT_TIMEOUT_MS = 30_000L }
}

class AutoLockObserver(
    private val store: SecurityStore,
    private val policy: AutoLockPolicy = AutoLockPolicy(),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = policy.onBackground(clock())

    override fun onStart(owner: LifecycleOwner) {
        if (policy.onForeground(clock()) && store.isPinSet) store.lock()
    }
}
