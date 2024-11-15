package com.umain.basicandroidintegration.presentation

import com.umain.revolver.RevolverDefaultErrorHandler
import com.umain.revolver.RevolverEffect
import com.umain.revolver.RevolverViewModel
import kotlinx.coroutines.delay

/**
 * Basic implementation of RevolverViewModel capable of handling events,
 * managing errors, and emitting states.
 */
class MainViewModel : RevolverViewModel<MainViewEvent, MainViewState, RevolverEffect>(
    initialState = MainViewState.Loading
) {
    init {
        /**
         * Sets up the event handler for ViewReady, the only defined event for this ViewModel.
         */
        addEventHandler<MainViewEvent.ViewReady> { event, emit ->
            val parameters = event.parameters
            /**
             * Delay here is just to show loading state and make state changes visible.
             */
            delay(1000)

            /**
             * If you want to trigger error handler then uncomment below line.
             */
            // throw Exception("Custom error message")

            emit.state(
                MainViewState.Loaded("Main Content for requested parameters: $parameters")
            )
        }

        /**
         * Configures the default error handler to emit a custom error state. However,
         * all errors will be mapped to the same state in this case.
         *
         * If you want to map different errors to different states, you need to implement
         * custom error handler, but this topic is not covered in this minimal implementation
         * example.
         */
        addErrorHandler(
            RevolverDefaultErrorHandler(
                MainViewState.Error("Error message")
            )
        )
    }
}
