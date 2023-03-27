package com.mcdonalds.lib.mvi.flow

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

actual open class CFlow<out T : Any> actual constructor(
    private val flow: Flow<T>,
) : Flow<T> by flow {

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
        @Suppress("OPT_IN_USAGE")
        return watch(
            coroutineScope = GlobalScope,
            dispatcher = Dispatchers.Main,
            onNext = onNext,
        )
    }
}
