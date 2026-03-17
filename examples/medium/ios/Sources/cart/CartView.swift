import SwiftUI
import shared // Replace with your KMP framework module name

private let catalogue: [Product] = [
    Product(id: "p1", name: "Kotlin In Action",    price: 49.99),
    Product(id: "p2", name: "Clean Architecture",  price: 39.99),
    Product(id: "p3", name: "Design Patterns",     price: 34.99),
    Product(id: "p4", name: "Refactoring",         price: 44.99),
]

/// Cart screen: product catalogue, cart line items with quantity controls,
/// order summary, and checkout — driven by `CartObservableViewModel`.
struct CartView: View {

    @ObservedObject var viewModel: CartObservableViewModel

    var body: some View {
        VStack(spacing: 0) {
            List {
                catalogueSection
                if !viewModel.state.items.isEmpty {
                    cartSection
                }
            }
            .listStyle(.insetGrouped)

            OrderSummary(
                state: viewModel.state,
                onCheckout: viewModel.checkout
            )
        }
        .alert("Order Placed! 🎉", isPresented: $viewModel.showOrderConfirmation) {
            Button("OK", role: .cancel) {}
        }
        .alert("Error", isPresented: .init(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    // MARK: - Sections

    @ViewBuilder
    private var catalogueSection: some View {
        Section("Catalogue") {
            ForEach(catalogue, id: \.id) { product in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(product.name).font(.body)
                        Text(product.price.formatted(.currency(code: "USD")))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button {
                        viewModel.add(product: product)
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.blue)
                }
            }
        }
    }

    @ViewBuilder
    private var cartSection: some View {
        Section("Your Cart") {
            ForEach(viewModel.state.items, id: \.product.id) { item in
                CartItemRow(item: item, viewModel: viewModel)
            }
        }
    }
}

// MARK: - Cart item row

private struct CartItemRow: View {
    let item: CartItem
    let viewModel: CartObservableViewModel

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.product.name).font(.body)
                Text(item.subtotal.formatted(.currency(code: "USD")))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            // Quantity stepper
            HStack(spacing: 4) {
                Button {
                    viewModel.updateQuantity(
                        productId: item.product.id,
                        newQuantity: item.quantity - 1
                    )
                } label: {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.plain)

                Text("\(item.quantity)")
                    .frame(minWidth: 24)
                    .monospacedDigit()

                Button {
                    viewModel.updateQuantity(
                        productId: item.product.id,
                        newQuantity: item.quantity + 1
                    )
                } label: {
                    Image(systemName: "plus.circle")
                }
                .buttonStyle(.plain)
            }
            .foregroundStyle(.blue)

            Button {
                viewModel.remove(productId: item.product.id)
            } label: {
                Image(systemName: "trash")
                    .foregroundStyle(.red)
            }
            .buttonStyle(.plain)
            .padding(.leading, 8)
        }
    }
}

// MARK: - Order summary

private struct OrderSummary: View {
    let state: CartState
    let onCheckout: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Divider()

            VStack(spacing: 4) {
                SummaryRow(label: "Subtotal", value: state.subtotal.formatted(.currency(code: "USD")))
                SummaryRow(label: "Tax (15 %)", value: state.tax.formatted(.currency(code: "USD")))
                SummaryRow(label: "Total", value: state.total.formatted(.currency(code: "USD")), bold: true)
            }
            .padding(.horizontal)

            Button(action: onCheckout) {
                if state.isCheckingOut {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Checkout")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(state.isCheckingOut)
            .padding(.horizontal)
            .padding(.bottom)
        }
        .background(.bar)
    }
}

private struct SummaryRow: View {
    let label: String
    let value: String
    var bold: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .fontWeight(bold ? .bold : .regular)
            Spacer()
            Text(value)
                .fontWeight(bold ? .bold : .regular)
                .monospacedDigit()
        }
    }
}
