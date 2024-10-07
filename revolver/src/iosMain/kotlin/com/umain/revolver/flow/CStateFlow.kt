package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

actual open class CStateFlow<out T : Any> actual constructor(
    private val flow: StateFlow<T>,
    private val coroutineScope: CoroutineScope,
    ) : CSharedFlow<T>(flow,coroutineScope), StateFlow<T> {
    actual override val value: T get() = flow.value

    actual override val replayCache: List<T>
        get() = super.replayCache

    actual override suspend fun collect(collector: FlowCollector<T>): Nothing {
        super.collect(collector)
    }
}
