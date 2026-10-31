package com.finctrl.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.UiMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val errorHandler: ErrorHandler) : ViewModel() {
    private val _events = Channel<UiMessage>(Channel.BUFFERED)
    val events: Flow<UiMessage> = _events.receiveAsFlow()

    fun notify(text: String) { _events.trySend(UiMessage(text)) }

    protected fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _events.trySend(UiMessage(errorHandler.toMessage(e), retry = { launchSafely(block) }))
            }
        }
    }

    protected fun <T> Flow<T>.safe(): Flow<T> = catch { _events.trySend(UiMessage(errorHandler.toMessage(it))) }
}
