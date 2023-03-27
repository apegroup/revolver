package com.mcdonalds.lib.mvi

import com.mcdonalds.lib.mvi.flow.CSharedFlow
import com.mcdonalds.lib.mvi.flow.CStateFlow

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
