package com.umain.revolver

import com.umain.revolver.flow.CSharedFlow
import com.umain.revolver.flow.CStateFlow

interface ViewModelInterface<EVENT : Event, STATE : State, EFFECT : Effect> {

    /**
     * Stateflow for observing state changes
     */
    val state: CStateFlow<STATE>

    /**
     * SharedFlow for observing side effects. Used for one of events like "Move to the next screen"
     */
    val effect: CSharedFlow<EFFECT>

    /**
     * adds a new [Event] to this viewModel
     */
    fun emit(event: EVENT)
}
