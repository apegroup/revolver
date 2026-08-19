package com.umain.mediumandroidintegration.auth

/**
 * Contract for authentication logic.
 *
 * Inject a real implementation in production; use [FakeAuthRepository] in examples/tests.
 */
interface AuthRepository {

    /**
     * Authenticates with the given credentials.
     *
     * @return the display name of the authenticated user.
     * @throws Exception if authentication fails.
     */
    suspend fun login(username: String, password: String): String
}
