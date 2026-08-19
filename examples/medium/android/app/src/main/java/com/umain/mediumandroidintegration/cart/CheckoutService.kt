package com.umain.mediumandroidintegration.cart

/**
 * Contract for checkout / payment processing.
 *
 * Inject a real implementation in production; use [FakeCheckoutService] in examples/tests.
 */
interface CheckoutService {

    /**
     * Process a payment for the given [amount].
     *
     * @throws Exception if payment fails.
     */
    suspend fun processCheckout(amount: Double)
}
