package com.umain.mediumandroidintegration.cart

/**
 * A product available for purchase.
 *
 * @param id stable identifier used to correlate cart items.
 * @param name human-readable product label.
 * @param price unit price in the local currency.
 */
data class Product(val id: String, val name: String, val price: Double)

/**
 * A [Product] with an associated quantity in the shopping cart.
 *
 * @property subtotal derived price for this line: `product.price × quantity`.
 */
data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val subtotal: Double get() = product.price * quantity
}
