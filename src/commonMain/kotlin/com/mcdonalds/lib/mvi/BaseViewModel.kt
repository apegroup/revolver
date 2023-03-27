package com.mcdonalds.lib.mvi

import kotlinx.coroutines.CoroutineScope

expect open class BaseViewModel() {
    val viewModelScope: CoroutineScope

    fun onCleared()
}
