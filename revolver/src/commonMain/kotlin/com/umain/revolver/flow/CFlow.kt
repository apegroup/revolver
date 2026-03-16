package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

/**
 * Platform-aware wrapper around [Flow] that adds a Swift-friendly [watch][com.umain.revolver.flow.CFlow.watch]
 * method on iOS, while remaining a plain [Flow] delegate on Android and JVM.
 *
 * Use [cFlow] to wrap an existing flow, or obtain one via [RevolverViewModelInterface.state] /
 * [RevolverViewModelInterface.effect].
 *
 * @param T the type of values emitted by this flow.
 */
expect class CFlow<out T : Any>(flow: Flow<T>, coroutineScope: CoroutineScope) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>)
}

/** Wraps this [Flow] in a [CFlow] using the given [coroutineScope]. */
@Suppress("OPT_IN_USAGE")
fun <T : Any> Flow<T>.cFlow(coroutineScope:  CoroutineScope = GlobalScope): CFlow<T> = CFlow(this, coroutineScope)
