package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-aware wrapper around [StateFlow]. Always holds the latest [value] and replays it
 * to new collectors. On iOS, inherits [CFlow.watch] for Swift observation.
 * Exposed as [RevolverViewModelInterface.state].
 *
 * @param T the type of the state value.
 */
expect open class CStateFlow<out T : Any>(flow: StateFlow<T>, coroutineScope: CoroutineScope) : StateFlow<T> {
    override val value: T
    override val replayCache: List<T>
    override suspend fun collect(collector: FlowCollector<T>): Nothing
}

/** Wraps this [StateFlow] in a [CStateFlow] using the given [coroutineScope]. */
@Suppress("OPT_IN_USAGE")
fun <T : Any> StateFlow<T>.cStateFlow(coroutineScope: CoroutineScope = GlobalScope): CStateFlow<T> = CStateFlow(this,coroutineScope)
