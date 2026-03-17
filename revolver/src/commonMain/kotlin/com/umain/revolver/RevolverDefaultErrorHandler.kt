package com.umain.revolver

import io.github.aakira.napier.Napier

/**
 * Built-in catch-all [RevolverErrorHandler] that maps any [Throwable] directly to a fixed
 * error state. Use this when you want a single generic error state without custom logic.
 *
 * ```kotlin
 * init {
 *     addErrorHandler(RevolverDefaultErrorHandler(ExampleState.Error("Something went wrong")))
 * }
 * ```
 *
 * @param STATE the [RevolverState] type of the target ViewModel.
 * @param EFFECT the [RevolverEffect] type of the target ViewModel.
 * @param genericErrorState the state to emit whenever any exception is caught.
 */
class RevolverDefaultErrorHandler<STATE, EFFECT>(
    private val genericErrorState: STATE,
) : RevolverErrorHandler<STATE, EFFECT, Throwable> {

    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        Napier.e("exception caught by viewmodel", exception)
        emit.state(genericErrorState)
    }
}
