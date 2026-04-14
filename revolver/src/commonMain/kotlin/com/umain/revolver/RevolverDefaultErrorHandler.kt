package com.umain.revolver

import io.github.aakira.napier.Napier

/**
 * A simple implementation of [RevolverErrorHandler] that maps any exception
 * directly to a specified error state.
 *
 * This handler logs the exception using Napier and emits the [genericErrorState].
 * It is useful for a uniform error handling experience when specific details
 * of the exception are not critical to the UI.
 *
 * @param genericErrorState The state to be emitted whenever any [Throwable] is caught.
 */
class RevolverDefaultErrorHandler<STATE, EFFECT>(
    private val genericErrorState: STATE,
) : RevolverErrorHandler<STATE, EFFECT, Throwable> {

    /**
     * Handles the [exception] by logging it and emitting the pre-configured [genericErrorState].
     *
     * @param exception The caught exception.
     * @param emit The emitter used to set the error state.
     */
    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        Napier.e("Exception caught in ViewModel", exception)
        emit.state(genericErrorState)
    }
}
