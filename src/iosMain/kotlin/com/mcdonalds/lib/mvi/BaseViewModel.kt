package com.mcdonalds.lib.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    actual fun onCleared() {
        viewModelScope.cancel()
    }
}
