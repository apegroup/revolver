# Medium iOS Integration Example

SwiftUI app that mirrors the medium Android example — three tabbed screens each
powered by an independent `ObservableObject` wrapper around a Kotlin ViewModel.

## Screens at a glance

| Tab | Kotlin ViewModel | Key Swift patterns |
|---|---|---|
| **Auth** | `AuthViewModel` | `ShowToast` → `@Published toastMessage` → `ToastBanner`; `NavigateToDashboard` stubbed for coordinator |
| **Search** | `SearchViewModel` | `ScrollToTop` effect → `scrollTrigger` counter → `onChange` + `ScrollViewProxy`; paginated `List` with load-more footer |
| **Cart** | `CartViewModel` | `data class` state accessed directly (no subtype switch); `OrderPlacedSuccessfully` / `ShowError` → `alert` modifiers |

## Setup

1. Build the XCFramework: `./gradlew :revolver:assembleXCFramework`
2. Create an Xcode project (iOS App / SwiftUI / Swift)
3. Copy **all** files from `Sources/` into the project (preserve folder structure)
4. Embed `revolver.xcframework` under **Frameworks, Libraries, and Embedded Content**
5. Replace `import shared` with `import revolver` (or your module name)
6. Run on an iOS 16+ simulator

## Architecture

```
ContentView (TabView)
├── AuthView        ← @StateObject AuthObservableViewModel(FakeAuthRepository)
├── SearchView      ← @StateObject SearchObservableViewModel(FakeSearchRepository)
└── CartView        ← @StateObject CartObservableViewModel(FakeCheckoutService)
```

ViewModels are `@StateObject` in `ContentView` so state survives tab switches —
the same approach as `MainActivity` in the Android example.

## Fake implementations

| Class | Simulates |
|---|---|
| `FakeAuthRepository` | 1 s delay; password `"fail"` throws |
| `FakeSearchRepository` | 700 ms delay; 2 pages × 10 results, then end-of-results |
| `FakeCheckoutService` | 1.2 s delay; always succeeds (uncomment `throw` to test failure) |

## Effect bridging patterns

### Toast — `@Published String?` + `ToastBanner` overlay
```swift
// ViewModel
@Published var toastMessage: String? = nil
// effect handler:
case let toast as AuthEffect.ShowToast: self?.toastMessage = toast.text

// View
ZStack(alignment: .bottom) {
    AuthView(viewModel: vm)
    ToastBanner(message: $vm.toastMessage)  // auto-dismisses after 2.5 s
}
```

### Scroll-to-top — counter trigger + `ScrollViewProxy`
```swift
// ViewModel
@Published private(set) var scrollTrigger: Int = 0
// effect handler:
case is SearchEffect.ScrollToTop: self?.scrollTrigger += 1

// View
.onChange(of: viewModel.scrollTrigger) { _ in
    withAnimation { scrollProxy?.scrollTo(0, anchor: .top) }
}
```

### Alert — `@Published Bool` / `@Published String?`
```swift
// ViewModel
@Published var showOrderConfirmation = false
@Published var errorMessage: String? = nil

// View
.alert("Order Placed! 🎉", isPresented: $viewModel.showOrderConfirmation) { ... }
.alert("Error", isPresented: .init(get: { vm.errorMessage != nil }, ...) { ... }
```

## Key files

```
Sources/
├── RevolverExampleApp.swift        # @main entry point
├── ContentView.swift               # TabView, @StateObject ViewModels, Fakes wiring
├── ToastBanner.swift               # Reusable self-dismissing toast overlay
├── Fakes.swift                     # FakeAuthRepository, FakeSearchRepository, FakeCheckoutService
├── auth/
│   ├── AuthObservableViewModel.swift
│   └── AuthView.swift              # LoginForm, AuthenticatedView
├── search/
│   ├── SearchObservableViewModel.swift  # scrollTrigger counter pattern
│   └── SearchView.swift                 # SearchBar, ResultsList, load-more footer
└── cart/
    ├── CartObservableViewModel.swift    # data class state, alert-based effects
    └── CartView.swift                   # Catalogue section, CartItemRow, OrderSummary
```
