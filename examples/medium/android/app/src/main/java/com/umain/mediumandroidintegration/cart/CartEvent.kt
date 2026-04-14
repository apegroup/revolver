package com.umain.mediumandroidintegration.cart

import com.umain.revolver.RevolverEvent

/**
 * Actions the user can send to [CartViewModel].
 */
sealed class CartEvent : RevolverEvent {

    /**
     * Add one unit of [product] to the cart.
     * Increments quantity if the product is already present.
     */
    data class AddItem(val product: Product) : CartEvent()

    /**
     * Remove a product from the cart entirely.
     *
     * @param productId the [Product.id] to remove.
     */
    data class RemoveItem(val productId: String) : CartEvent()

    /**
     * Set the quantity of a specific product.
     * Removes the item when [newQuantity] is ≤ 0.
     *
     * @param productId the [Product.id] to update.
     * @param newQuantity the desired new quantity.
     */
    data class UpdateQuantity(val productId: String, val newQuantity: Int) : CartEvent()

    /** Begin the checkout process for the current cart. */
    data object Checkout : CartEvent()
}
