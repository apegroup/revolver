package com.mcdonalds.lib.mvi.flow

import kotlinx.coroutines.flow.SharedFlow

expect open class CSharedFlow<out T : Any>(flow: SharedFlow<T>) : SharedFlow<T>

fun <T : Any> SharedFlow<T>.cSharedFlow(): CSharedFlow<T> = CSharedFlow(this)
