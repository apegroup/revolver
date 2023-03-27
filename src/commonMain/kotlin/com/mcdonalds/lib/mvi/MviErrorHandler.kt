package com.mcdonalds.lib.mvi

interface MviErrorHandler<STATE, EFFECT, ERROR> {

    suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
}
