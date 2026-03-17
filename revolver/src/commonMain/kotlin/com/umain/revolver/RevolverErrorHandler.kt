package com.umain.revolver

/**
 * Interface for reusable error handlers that can be shared across multiple ViewModels.
 *
 * Implement this interface when the same error handling logic (e.g. mapping a
 * `NetworkException` to an offline state) is needed in more than one ViewModel.
 * Register instances via [RevolverViewModel.addErrorHandler].
 *
 * ```kotlin
 * class NetworkErrorHandler<STATE : RevolverState, EFFECT : RevolverEffect>(
 *     private val offlineState: STATE,
 * ) : RevolverErrorHandler<STATE, EFFECT, NetworkException> {
 *     override suspend fun handleError(exception: NetworkException, emit: Emitter<STATE, EFFECT>) {
 *         emit.state(offlineState)
 *     }
 * }
 * ```
 *
 * @param STATE the [RevolverState] type of the target ViewModel.
 * @param EFFECT the [RevolverEffect] type of the target ViewModel.
 * @param ERROR the exception type this handler responds to.
 */
interface RevolverErrorHandler<STATE, EFFECT, ERROR> {

    /**
     * Called when an exception of type [ERROR] is caught by the ViewModel.
     *
     * @param exception the caught exception.
     * @param emit use to push new states or effects in response to the error.
     */
    suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
}
