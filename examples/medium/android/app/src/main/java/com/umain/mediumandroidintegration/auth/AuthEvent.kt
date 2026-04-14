package com.umain.mediumandroidintegration.auth

import com.umain.revolver.RevolverEvent

/**
 * Actions the user or system can send to [AuthViewModel].
 */
sealed class AuthEvent : RevolverEvent {

    /**
     * Submit login credentials.
     *
     * @param username the entered username — handler validates it is not blank.
     * @param password the entered password — handler validates it is not blank.
     */
    data class Login(val username: String, val password: String) : AuthEvent()

    /** Log out the currently authenticated user. */
    data object Logout : AuthEvent()
}
