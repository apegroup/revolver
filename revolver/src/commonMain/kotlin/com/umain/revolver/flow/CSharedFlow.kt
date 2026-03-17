package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow

/**
 * Platform-aware wrapper around [SharedFlow]. On iOS, inherits [CFlow.watch] for Swift observation.
 * Exposed as [RevolverViewModelInterface.effect].
 *
 * @param T the type of values emitted by this flow.
 */
expect open class CSharedFlow<out T : Any>(flow: SharedFlow<T>, coroutineScope: CoroutineScope) : SharedFlow<T> {
    override val replayCache: List<T>
    override suspend fun collect(collector: FlowCollector<T>): Nothing
}

/** Wraps this [SharedFlow] in a [CSharedFlow] using the given [coroutineScope]. */
@Suppress("OPT_IN_USAGE")
fun <T : Any> SharedFlow<T>.cSharedFlow(coroutineScope:  CoroutineScope = GlobalScope): CSharedFlow<T> = CSharedFlow(this, coroutineScope)
