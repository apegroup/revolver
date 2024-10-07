package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

actual open class CFlow<out T : Any> actual constructor(
    private val flow: Flow<T>,
    private val coroutineScope: CoroutineScope,
) : Flow<T> by flow {
    actual override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }

    private fun watch(
        coroutineScope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        onNext: (T) -> Unit,
    ): DisposableHandle {
        val job: Job = coroutineScope.launch(dispatcher) {
            flow.onEach { onNext(it) }.collect()
        }
        return DisposableHandle {
            job.cancel()
        }
    }

    fun watch(onNext: (T) -> Unit): DisposableHandle {
        return watch(
            coroutineScope = coroutineScope,
            dispatcher = Dispatchers.Main,
            onNext = onNext,
        )
    }
}
