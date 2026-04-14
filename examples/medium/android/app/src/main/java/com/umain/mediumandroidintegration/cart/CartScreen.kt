package com.umain.mediumandroidintegration.cart

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private val CATALOGUE = listOf(
    Product("p1", "Kotlin In Action", 49.99),
    Product("p2", "Clean Architecture", 39.99),
    Product("p3", "Design Patterns", 34.99),
    Product("p4", "Refactoring", 44.99),
)

/**
 * Cart screen: product catalogue to add items, cart line items with quantity controls,
 * order summary, and checkout — driven by [CartViewModel].
 */
@Composable
fun CartScreen(viewModel: CartViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(CartState()) }

    LaunchedEffect(Unit) {
        viewModel.state.collect { uiState = it }
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CartEffect.OrderPlacedSuccessfully ->
                    Toast.makeText(context, "Order placed! 🎉", Toast.LENGTH_LONG).show()
                is CartEffect.ShowError ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Catalogue ──────────────────────────────────────────────────
            item {
                Text("Catalogue", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
            items(CATALOGUE) { product ->
                CatalogueRow(
                    product = product,
                    onAdd = { viewModel.emit(CartEvent.AddItem(product)) },
                )
            }

            // ── Cart ───────────────────────────────────────────────────────
            if (uiState.items.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Your Cart", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                }
                items(uiState.items) { cartItem ->
                    CartItemRow(
                        item = cartItem,
                        onIncrement = {
                            viewModel.emit(CartEvent.UpdateQuantity(cartItem.product.id, cartItem.quantity + 1))
                        },
                        onDecrement = {
                            viewModel.emit(CartEvent.UpdateQuantity(cartItem.product.id, cartItem.quantity - 1))
                        },
                        onRemove = {
                            viewModel.emit(CartEvent.RemoveItem(cartItem.product.id))
                        },
                    )
                }
            }
        }

        // ── Order summary + checkout ───────────────────────────────────────
        OrderSummary(
            state = uiState,
            onCheckout = { viewModel.emit(CartEvent.Checkout) },
        )
    }
}

@Composable
private fun CatalogueRow(product: Product, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium)
                Text(product.price.formatPrice(), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add ${product.name}")
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.Medium)
                Text(item.subtotal.formatPrice(), style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = onDecrement) { Text("−") }
            Text("${item.quantity}", modifier = Modifier.width(24.dp))
            IconButton(onClick = onIncrement) {
                Icon(Icons.Default.Add, contentDescription = "Increment")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${item.product.name}")
            }
        }
    }
}

@Composable
private fun OrderSummary(state: CartState, onCheckout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SummaryRow("Subtotal", state.subtotal.formatPrice())
            SummaryRow("Tax (15 %)", state.tax.formatPrice())
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Total", state.total.formatPrice(), bold = true)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCheckout,
                enabled = !state.isCheckingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isCheckingOut) "Processing…" else "Checkout")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(value, fontWeight = if (bold) FontWeight.Bold else null)
    }
}

private fun Double.formatPrice(): String = String.format(Locale.US, "$%.2f", this)
