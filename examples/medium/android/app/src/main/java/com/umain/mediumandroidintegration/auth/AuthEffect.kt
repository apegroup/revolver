package com.umain.mediumandroidintegration.auth

import com.umain.revolver.RevolverEffect

/**
 * One-shot side effects emitted by [AuthViewModel].
 */
sealed class AuthEffect : RevolverEffect {

    /** Navigate to the dashboard after a successful login. */
    data object NavigateToDashboard : AuthEffect()

    /**
     * Display a brief toast notification.
     *
     * @param text the message to show.
     */
    data class ShowToast(val text: String) : AuthEffect()
}
