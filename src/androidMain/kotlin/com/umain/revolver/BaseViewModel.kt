package com.umain.revolver

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() : ViewModel() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
