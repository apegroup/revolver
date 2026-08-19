package com.umain.advancedandroidintegration.presentation

import com.umain.revolver.RevolverEffect

/**
 * One-shot side effects emitted by [MainViewModel].
 *
 * Effects are not retained — a subscriber that connects after the effect fires will not
 * receive it. Use them for actions that must happen exactly once (toasts, navigation, etc.).
 */
sealed class MainViewEffect : RevolverEffect {

    /**
     * Request the UI to show a short toast message.
     *
     * @param message the text to display.
     */
    data class ShowToast(val message: String) : MainViewEffect()
}
