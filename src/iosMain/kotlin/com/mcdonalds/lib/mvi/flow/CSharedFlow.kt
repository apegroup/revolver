package com.mcdonalds.lib.mvi.flow

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharedFlow

actual open class CSharedFlow<out T : Any> actual constructor(
    private val flow: SharedFlow<T>,
) : CFlow<T>(flow), SharedFlow<T> {
    override val replayCache: List<T> get() = flow.replayCache

    override suspend fun collect(collector: FlowCollector<T>): Nothing = flow.collect(collector)
}
