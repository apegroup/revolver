package com.umain.mediumandroidintegration.cart

import com.umain.revolver.RevolverState

/**
 * Immutable snapshot of the shopping cart.
 *
 * Uses a single `data class` (rather than a sealed class) because the cart is always
 * visible — only its contents change. Derivative totals are kept in-state so the UI
 * never needs to recompute them.
 *
 * @param items current line items.
 * @param isCheckingOut true while the checkout request is in-flight.
 * @param subtotal sum of all [CartItem.subtotal] values.
 * @param tax 15 % tax applied on [subtotal].
 * @param total [subtotal] + [tax].
 */
data class CartState(
    val items: List<CartItem> = emptyList(),
    val isCheckingOut: Boolean = false,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
) : RevolverState
