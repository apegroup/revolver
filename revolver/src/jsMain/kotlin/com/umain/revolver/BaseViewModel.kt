package com.umain.revolver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    fun dispose() = viewModelScope.cancel()
}
