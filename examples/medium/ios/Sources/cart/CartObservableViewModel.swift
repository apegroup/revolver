import Foundation
import shared // Replace with your KMP framework module name

/// SwiftUI-compatible wrapper around the Kotlin `CartViewModel`.
///
/// The Kotlin `CartState` is a `data class` (not a sealed class), so `state` is
/// always non-nil and the view renders directly from its properties rather than
/// switching on subtypes.
///
/// Effects are bridged as:
/// - `OrderPlacedSuccessfully` → `showOrderConfirmation = true` (drives an alert)
/// - `ShowError` → `errorMessage: String?` (drives an alert)
final class CartObservableViewModel: ObservableObject {

    // MARK: - Published state

    @Published private(set) var state: CartState = CartState()

    /// Drives the "Order placed!" confirmation alert.
    @Published var showOrderConfirmation = false

    /// Non-nil while an error alert should be displayed.
    @Published var errorMessage: String? = nil

    // MARK: - Private

    private let viewModel: CartViewModel
    private var handle: (any DisposableHandle)?

    // MARK: - Init / deinit

    init(checkoutService: any CheckoutService) {
        self.viewModel = CartViewModel(checkoutService: checkoutService)

        let stateHandle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }

        let effectHandle = viewModel.effect.watch { [weak self] effect in
            switch effect {
            case is CartEffect.OrderPlacedSuccessfully:
                self?.showOrderConfirmation = true
            case let err as CartEffect.ShowError:
                self?.errorMessage = err.message
            default:
                break
            }
        }

        handle = stateHandle + effectHandle
    }

    deinit {
        handle?.dispose()
        viewModel.dispose()
    }

    // MARK: - Intent forwarding

    func add(product: Product) {
        viewModel.emit(event: CartEvent.AddItem(product: product))
    }

    func remove(productId: String) {
        viewModel.emit(event: CartEvent.RemoveItem(productId: productId))
    }

    func updateQuantity(productId: String, newQuantity: Int32) {
        viewModel.emit(event: CartEvent.UpdateQuantity(productId: productId, newQuantity: newQuantity))
    }

    func checkout() {
        viewModel.emit(event: CartEvent.Checkout())
    }
}
