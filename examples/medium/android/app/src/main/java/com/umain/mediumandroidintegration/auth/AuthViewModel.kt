package com.umain.mediumandroidintegration.auth

import com.umain.revolver.Emitter
import com.umain.revolver.RevolverViewModel

/**
 * Manages the authentication flow.
 *
 * Demonstrates:
 * - Client-side validation before hitting the repository
 * - Chained state transitions: Idle → Authenticating → Authenticated / Error
 * - Both state and effect emitted from the same handler
 * - Inline error handler lambda as an alternative to a named class
 *
 * @param repository provides the actual authentication logic.
 */
class AuthViewModel(
    private val repository: AuthRepository,
) : RevolverViewModel<AuthEvent, AuthState, AuthEffect>(
    initialState = AuthState.Idle,
) {

    init {
        addEventHandler<AuthEvent.Login>(::onLogin)
        addEventHandler<AuthEvent.Logout>(::onLogout)

        // Inline error handler: catches anything not handled in onLogin's own try/catch.
        addErrorHandler<Exception> { exception, emit ->
            emit.state(AuthState.Error(exception.message ?: "An unexpected error occurred."))
        }
    }

    private suspend fun onLogin(event: AuthEvent.Login, emit: Emitter<AuthState, AuthEffect>) {
        if (event.username.isBlank() || event.password.isBlank()) {
            emit.effect(AuthEffect.ShowToast("Please fill in all fields."))
            return
        }

        emit.state(AuthState.Authenticating)

        try {
            val user = repository.login(event.username, event.password)
            emit.state(AuthState.Authenticated(user))
            emit.effect(AuthEffect.NavigateToDashboard)
        } catch (e: Exception) {
            emit.state(AuthState.Error(e.message ?: "Invalid credentials."))
        }
    }

    private suspend fun onLogout(event: AuthEvent.Logout, emit: Emitter<AuthState, AuthEffect>) {
        emit.state(AuthState.Idle)
        emit.effect(AuthEffect.ShowToast("Logged out successfully."))
    }
}
