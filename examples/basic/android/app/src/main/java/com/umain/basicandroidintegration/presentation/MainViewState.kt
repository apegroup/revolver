package com.umain.basicandroidintegration.presentation

import com.umain.revolver.RevolverState

/**
 * MainViewState defines all possible states that the ViewModel can emit to the UI.
 */
sealed class MainViewState : RevolverState {

    /**
     * Initial loading state shown to the user while awaiting data from the ViewModel.
     * This could be a spinner, animation, or any visual indicator readily available in the
     * app package, aimed at preventing user confusion or the impression that the app has frozen.
     */
    data object Loading : MainViewState()

    /**
     * Loaded state emitted when the ViewModel has successfully loaded data.
     * This might take time, but having initial state takes care about user while the data
     * is loading of processing.
     *
     * UI should initiate data loading by sending an appropriate event to the ViewModel. In
     * this example it is [MainViewEvent.ViewReady].
     */
    data class Loaded(
        val welcomeMessage: String
    ) : MainViewState()

    /**
     * In this example, all errors are handled within the ViewModel, and only this error state
     * is emitted to the UI.
     *
     * It's recommended to keep error handling straightforward.
     *
     * If the logic becomes complex, consider using a custom error handler to manage errors
     * and emit specific states that the UI can directly process without extra handling steps.
     */
    data class Error(
        val errorMessage: String
    ) : MainViewState()
}
