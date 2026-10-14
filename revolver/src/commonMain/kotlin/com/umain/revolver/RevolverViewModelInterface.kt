package com.umain.revolver

import com.umain.revolver.flow.CSharedFlow
import com.umain.revolver.flow.CStateFlow
import com.umain.revolver.flow.PlainSharedFlow
import com.umain.revolver.flow.PlainStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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
     * The same flow as [state], typed as a plain [StateFlow].
     *
     * Use this from Swift when the framework is built with Swift export, which bridges
     * [StateFlow] to an `AsyncSequence`. Swift export does not bridge the [CStateFlow] wrapper.
     * Objective-C export users should keep using [state] and `watch`.
     */
    val stateFlow: StateFlow<STATE> get() = PlainStateFlow(state)

    /**
     * The same flow as [effect], typed as a plain [SharedFlow].
     *
     * Use this from Swift when the framework is built with Swift export, which bridges
     * [SharedFlow] to an `AsyncSequence`. Swift export does not bridge the [CSharedFlow] wrapper.
     * Objective-C export users should keep using [effect] and `watch`.
     */
    val effectFlow: SharedFlow<EFFECT> get() = PlainSharedFlow(effect)

    /**
     * Sends [event] to the ViewModel for asynchronous processing by its registered
     * [EventHandler].
     *
     * @param event the [RevolverEvent] to dispatch.
     */
    fun emit(event: EVENT)
}
