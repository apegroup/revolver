package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow

expect open class CSharedFlow<out T : Any>(flow: SharedFlow<T>, coroutineScope: CoroutineScope) : SharedFlow<T> {
    override val replayCache: List<T>
    override suspend fun collect(collector: FlowCollector<T>): Nothing
}

@Suppress("OPT_IN_USAGE")
fun <T : Any> SharedFlow<T>.cSharedFlow(coroutineScope:  CoroutineScope = GlobalScope): CSharedFlow<T> = CSharedFlow(this, coroutineScope)
