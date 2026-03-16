package com.umain.revolver

import com.umain.revolver.flow.CSharedFlow
import com.umain.revolver.flow.CStateFlow

/**
 * Public contract for a Revolver ViewModel. Consumers should depend on this interface rather
 * than the concrete [RevolverViewModel] class to keep their code testable and decoupled.
 *
 * @param EVENT the [RevolverEvent] type accepted by [emit].
 * @param STATE the [RevolverState] type emitted by [state].
 * @param EFFECT the [RevolverEffect] type emitted by [effect].
 */
interface RevolverViewModelInterface<EVENT : RevolverEvent, STATE : RevolverState, EFFECT : RevolverEffect> {

    /**
     * The current view state. Always holds the last emitted [STATE] value and immediately
     * delivers it to new collectors.
     */
    val state: CStateFlow<STATE>

    /**
     * One-shot side effects such as navigation or toasts. Not replayed to late collectors.
     */
    val effect: CSharedFlow<EFFECT>

    /**
     * Sends [event] to the ViewModel for asynchronous processing by its registered
     * [EventHandler].
     *
     * @param event the [RevolverEvent] to dispatch.
     */
    fun emit(event: EVENT)
}
