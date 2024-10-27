package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

expect open class CStateFlow<out T : Any>(flow: StateFlow<T>, coroutineScope: CoroutineScope) : StateFlow<T> {
    override val value: T
    override val replayCache: List<T>
    override suspend fun collect(collector: FlowCollector<T>): Nothing
}

@Suppress("OPT_IN_USAGE")
fun <T : Any> StateFlow<T>.cStateFlow(coroutineScope: CoroutineScope = GlobalScope): CStateFlow<T> = CStateFlow(this,coroutineScope)
