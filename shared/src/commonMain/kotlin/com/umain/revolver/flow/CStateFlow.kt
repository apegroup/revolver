package com.umain.revolver.flow

import kotlinx.coroutines.flow.StateFlow

expect open class CStateFlow<out T : Any>(flow: StateFlow<T>) : StateFlow<T>

fun <T : Any> StateFlow<T>.cStateFlow(): CStateFlow<T> = CStateFlow(this)
