package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow

expect class CFlow<out T : Any>(flow: Flow<T>, coroutineScope: CoroutineScope) : Flow<T>

@Suppress("OPT_IN_USAGE")
fun <T : Any> Flow<T>.cFlow(coroutineScope:  CoroutineScope = GlobalScope): CFlow<T> = CFlow(this, coroutineScope)
