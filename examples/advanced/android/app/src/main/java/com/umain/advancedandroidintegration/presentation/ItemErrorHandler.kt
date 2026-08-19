package com.umain.advancedandroidintegration.presentation

import com.umain.revolver.Emitter
import com.umain.revolver.RevolverEffect
import com.umain.revolver.RevolverErrorHandler
import com.umain.revolver.RevolverState

/**
 * Reusable error handler that converts any [Exception] into an error state using a
 * caller-supplied factory lambda.
 *
 * This pattern avoids coupling the error handler to a specific [STATE] subtype while
 * still letting callers control what error state is emitted:
 *
 * ```kotlin
 * addErrorHandler(ItemErrorHandler { message -> MainViewState.Error(message) })
 * ```
 *
 * @param STATE the [RevolverState] type of the target ViewModel.
 * @param EFFECT the [RevolverEffect] type of the target ViewModel.
 * @param toErrorState factory that maps an error message to the appropriate [STATE].
 */
class ItemErrorHandler<STATE : RevolverState, EFFECT : RevolverEffect>(
    private val toErrorState: (message: String) -> STATE,
) : RevolverErrorHandler<STATE, EFFECT, Exception> {

    override suspend fun handleError(exception: Exception, emit: Emitter<STATE, EFFECT>) {
        emit.state(toErrorState(exception.message ?: "An unexpected error occurred"))
    }
}
