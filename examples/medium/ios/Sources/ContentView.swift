import SwiftUI
import shared // Replace with your KMP framework module name

/// Root view: three-tab app mirroring the medium Android example.
///
/// ViewModels are created once here (as `@StateObject`) so state survives tab switches —
/// the same approach used in the Android `MainActivity`.
struct ContentView: View {

    @StateObject private var authViewModel = AuthObservableViewModel(
        repository: FakeAuthRepository()
    )
    @StateObject private var searchViewModel = SearchObservableViewModel(
        repository: FakeSearchRepository()
    )
    @StateObject private var cartViewModel = CartObservableViewModel(
        checkoutService: FakeCheckoutService()
    )

    var body: some View {
        TabView {
            NavigationStack {
                AuthView(viewModel: authViewModel)
                    .navigationTitle("Auth")
            }
            .tabItem { Label("Auth", systemImage: "person.fill") }

            NavigationStack {
                SearchView(viewModel: searchViewModel)
                    .navigationTitle("Search")
            }
            .tabItem { Label("Search", systemImage: "magnifyingglass") }

            NavigationStack {
                CartView(viewModel: cartViewModel)
                    .navigationTitle("Cart")
            }
            .tabItem { Label("Cart", systemImage: "cart.fill") }
        }
    }
}
