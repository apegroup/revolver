package com.umain.mediumandroidintegration.fake

import com.umain.mediumandroidintegration.cart.CheckoutService
import kotlinx.coroutines.delay

/**
 * In-memory [CheckoutService] used in the example and unit tests.
 *
 * Always succeeds after a simulated 1.2-second processing delay.
 * To test failure paths, replace this with an implementation that throws.
 */
class FakeCheckoutService : CheckoutService {

    override suspend fun processCheckout(amount: Double) {
        delay(1_200L)
        // Uncomment to simulate a payment failure:
        // throw Exception("Card declined.")
    }
}
