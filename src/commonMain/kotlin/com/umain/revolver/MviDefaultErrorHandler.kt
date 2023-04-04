package com.umain.revolver

import io.github.aakira.napier.Napier

/**
 * simplest implementation of a reusable [MviErrorHandler]. will map any error directly
 * to the provided state
 */
class MviDefaultErrorHandler<STATE, EFFECT>(
    private val genericErrorState: STATE,
) : MviErrorHandler<STATE, EFFECT, Throwable> {

    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        Napier.e("exception caught by viewmodel", exception)
        emit.state(genericErrorState)
    }
}
