package com.umain.revolver

/**
 * interface for implementing error handlers that can be reused
 * and shared between multiple ViewModels
 */
interface RevolverErrorHandler<STATE, EFFECT, ERROR> {

    suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
}
