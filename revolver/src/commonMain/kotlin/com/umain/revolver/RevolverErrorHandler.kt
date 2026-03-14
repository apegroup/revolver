package com.umain.revolver

/**
 * Interface for implementing reusable and shareable error handlers across multiple ViewModels.
 *
 * Implementations of this interface can encapsulate complex error handling logic, such as
 * logging, analytics reporting, or mapping specific exceptions to common error states.
 *
 * @param STATE The type of states emitted by the ViewModel using this handler.
 * @param EFFECT The type of side effects emitted by the ViewModel using this handler.
 * @param ERROR The specific type of [Throwable] this handler handles.
 */
interface RevolverErrorHandler<STATE, EFFECT, ERROR> {

    /**
     * Processes the given [exception] and uses the [emit] to update state or emit effects.
     *
     * @param exception The caught exception to be handled.
     * @param emit The emitter to update state or trigger side effects in response to the error.
     */
    suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
}
