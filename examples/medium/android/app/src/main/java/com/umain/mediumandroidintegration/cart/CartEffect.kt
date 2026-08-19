package com.umain.mediumandroidintegration.cart

import com.umain.revolver.RevolverEffect

/**
 * One-shot side effects emitted by [CartViewModel].
 */
sealed class CartEffect : RevolverEffect {

    /** Order completed — navigate to confirmation or show a success banner. */
    data object OrderPlacedSuccessfully : CartEffect()

    /**
     * Something went wrong — show a brief error notification.
     *
     * @param message a human-readable failure reason.
     */
    data class ShowError(val message: String) : CartEffect()
}
