package com.umain.revolver

import com.umain.revolver.flow.CSharedFlow
import com.umain.revolver.flow.CStateFlow

/**
 * Interface representing the public API of a RevolverViewModel.
 *
 * This interface defines the contract for communicating with a ViewModel in the Revolver framework.
 * It provides observable streams for states and side effects, and a mechanism to emit events.
 *
 * @param EVENT The type of events this ViewModel can handle, must implement [RevolverEvent].
 * @param STATE The type of states this ViewModel can emit, must implement [RevolverState].
 * @param EFFECT The type of side effects this ViewModel can trigger, must implement [RevolverEffect].
 */
interface RevolverViewModelInterface<EVENT : RevolverEvent, STATE : RevolverState, EFFECT : RevolverEffect> {

    /**
     * A [CStateFlow] representing the current immutable state of the ViewModel.
     * Observers (clients) should subscribe to this to update the UI based on state changes.
     */
    val state: CStateFlow<STATE>

    /**
     * A [CSharedFlow] for observing one-time side effects (e.g., navigation, alerts).
     * Unlike [state], effects are not cached and are typically handled once by the client.
     */
    val effect: CSharedFlow<EFFECT>

    /**
     * Emits a new [RevolverEvent] to the ViewModel for processing.
     *
     * @param event The event to be handled by the ViewModel.
     */
    fun emit(event: EVENT)
}
