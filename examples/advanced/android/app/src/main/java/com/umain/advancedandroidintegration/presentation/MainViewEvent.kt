package com.umain.advancedandroidintegration.presentation

import com.umain.revolver.RevolverEvent

/**
 * All actions the UI can send to [MainViewModel].
 *
 * Keep event parameters minimal — only pass what the handler cannot derive on its own.
 */
sealed class MainViewEvent : RevolverEvent {

    /** Triggered on first launch or when the user taps Refresh. */
    data object LoadItems : MainViewEvent()

    /** Triggered when the user taps Retry after an error. */
    data object RetryAfterError : MainViewEvent()
}
