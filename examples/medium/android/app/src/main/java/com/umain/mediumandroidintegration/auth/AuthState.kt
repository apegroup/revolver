package com.umain.mediumandroidintegration.auth

import com.umain.revolver.RevolverState

/**
 * Immutable UI state for the authentication screen.
 */
sealed class AuthState : RevolverState {

    /** Form is visible and ready for input. */
    data object Idle : AuthState()

    /** Login request is in-flight — show a loading indicator. */
    data object Authenticating : AuthState()

    /**
     * Login succeeded.
     *
     * @param userName the display name returned by [AuthRepository].
     */
    data class Authenticated(val userName: String) : AuthState()

    /**
     * Login failed.
     *
     * @param message a human-readable failure reason.
     */
    data class Error(val message: String) : AuthState()
}
