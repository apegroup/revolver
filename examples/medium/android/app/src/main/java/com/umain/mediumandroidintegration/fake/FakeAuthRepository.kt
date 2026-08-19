package com.umain.mediumandroidintegration.fake

import com.umain.mediumandroidintegration.auth.AuthRepository
import kotlinx.coroutines.delay

/**
 * In-memory [AuthRepository] used in the example and unit tests.
 *
 * Accepts any non-blank username / password combination.
 * Simulates a 1-second network round-trip.
 */
class FakeAuthRepository : AuthRepository {

    override suspend fun login(username: String, password: String): String {
        delay(1_000L)
        if (password == "fail") throw Exception("Invalid credentials.")
        return username.replaceFirstChar { it.uppercase() }
    }
}
