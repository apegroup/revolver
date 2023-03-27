package com.mcdonalds.lib.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() : ViewModel() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    public actual override fun onCleared() {
        super.onCleared()

        viewModelScope.cancel()
    }
}
