package com.umain.mediumandroidintegration.cart

import com.umain.revolver.Emitter
import com.umain.revolver.RevolverViewModel

/**
 * Manages the shopping cart, quantity controls, and checkout flow.
 *
 * Demonstrates:
 * - A single-state `data class` model updated via `copy()` across every handler
 * - Derived state (subtotal / tax / total) recomputed in one place ([recalculateTotals])
 * - Event delegation: [UpdateQuantity] with qty ≤ 0 delegates to [RemoveItem]
 * - Pre-condition guard in [Checkout] (empty cart check)
 * - Injected [CheckoutService] for testability
 *
 * @param checkoutService handles the actual payment transaction.
 */
class CartViewModel(
    private val checkoutService: CheckoutService,
) : RevolverViewModel<CartEvent, CartState, CartEffect>(
    initialState = CartState(),
) {

    init {
        addEventHandler<CartEvent.AddItem>(::onAddItem)
        addEventHandler<CartEvent.RemoveItem>(::onRemoveItem)
        addEventHandler<CartEvent.UpdateQuantity>(::onUpdateQuantity)
        addEventHandler<CartEvent.Checkout>(::onCheckout)
    }

    private suspend fun onAddItem(event: CartEvent.AddItem, emit: Emitter<CartState, CartEffect>) {
        val updated = state.value.items.toMutableList()
        val idx = updated.indexOfFirst { it.product.id == event.product.id }

        if (idx != -1) {
            val existing = updated[idx]
            updated[idx] = existing.copy(quantity = existing.quantity + 1)
        } else {
            updated.add(CartItem(event.product, quantity = 1))
        }

        emit.state(recalculateTotals(updated))
    }

    private suspend fun onRemoveItem(event: CartEvent.RemoveItem, emit: Emitter<CartState, CartEffect>) {
        val updated = state.value.items.filter { it.product.id != event.productId }
        emit.state(recalculateTotals(updated))
    }

    private suspend fun onUpdateQuantity(
        event: CartEvent.UpdateQuantity,
        emit: Emitter<CartState, CartEffect>,
    ) {
        if (event.newQuantity <= 0) {
            // Delegate removal to onRemoveItem to keep logic in one place.
            onRemoveItem(CartEvent.RemoveItem(event.productId), emit)
            return
        }

        val updated = state.value.items.map {
            if (it.product.id == event.productId) it.copy(quantity = event.newQuantity) else it
        }
        emit.state(recalculateTotals(updated))
    }

    private suspend fun onCheckout(event: CartEvent.Checkout, emit: Emitter<CartState, CartEffect>) {
        if (state.value.items.isEmpty()) {
            emit.effect(CartEffect.ShowError("Your cart is empty."))
            return
        }

        emit.state(state.value.copy(isCheckingOut = true))

        try {
            checkoutService.processCheckout(state.value.total)
            emit.state(CartState()) // reset cart
            emit.effect(CartEffect.OrderPlacedSuccessfully)
        } catch (e: Exception) {
            emit.state(state.value.copy(isCheckingOut = false))
            emit.effect(CartEffect.ShowError(e.message ?: "Payment failed. Please try again."))
        }
    }

    /** Recomputes subtotal / tax / total from the given [items] list. */
    private fun recalculateTotals(items: List<CartItem>): CartState {
        val subtotal = items.sumOf { it.subtotal }
        val tax = subtotal * TAX_RATE
        return state.value.copy(
            items = items,
            subtotal = subtotal,
            tax = tax,
            total = subtotal + tax,
        )
    }

    companion object {
        private const val TAX_RATE = 0.15
    }
}
