package com.mcdonalds.lib.mvi.flow

import kotlinx.coroutines.flow.Flow

expect class CFlow<out T : Any>(flow: Flow<T>) : Flow<T>

fun <T : Any> Flow<T>.cFlow(): CFlow<T> = CFlow(this)
