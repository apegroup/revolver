# Medium Android Integration Example

A single-activity Android app with three tabbed screens that each showcase a different
real-world Revolver pattern. Build on top of the advanced example's library versions
and Compose setup — no additional dependencies.

## Screens at a glance

| Tab | Feature | Key patterns |
|---|---|---|
| **Auth** | Login / logout flow | Client-side validation before state change; chained transitions `Idle → Authenticating → Authenticated / Error`; inline `addErrorHandler` lambda |
| **Search** | Paginated list search | `state.value` read inside a handler; partial state update via `copy(isNextPageLoading = true)`; `LoadMore` idempotency guard |
| **Cart** | Shopping cart + checkout | Single `data class` state model; derived totals in `recalculateTotals()`; event delegation (`UpdateQuantity` → `RemoveItem`); injected `CheckoutService` |

## Running

```bash
export GH_USERNAME=<your-github-username>
export GH_TOKEN=<your-github-token-with-read:packages>

cd examples/medium/android
./gradlew installDebug
```

## Architecture

```
MainActivity
├── AuthScreen   ← AuthViewModel(FakeAuthRepository)
├── SearchScreen ← SearchViewModel(FakeSearchRepository)
└── CartScreen   ← CartViewModel(FakeCheckoutService)
```

ViewModels are activity-scoped properties so state survives tab switches without a
navigation or ViewModel factory setup.

## Fake implementations

| Class | Simulates |
|---|---|
| `FakeAuthRepository` | 1 s delay; password `"fail"` triggers an error |
| `FakeSearchRepository` | 700 ms delay; 2 pages × 10 results, then end-of-results |
| `FakeCheckoutService` | 1.2 s delay; always succeeds (uncomment throw to test failure) |

## Key files

```
app/src/main/java/com/umain/mediumandroidintegration/
├── MainActivity.kt                  # BottomNavBar, activity-scoped ViewModels
├── auth/
│   ├── AuthEvent.kt                 # Login(username, password), Logout
│   ├── AuthState.kt                 # Idle, Authenticating, Authenticated, Error
│   ├── AuthEffect.kt                # NavigateToDashboard, ShowToast
│   ├── AuthRepository.kt            # interface
│   ├── AuthViewModel.kt             # inline error handler, validation guard
│   └── AuthScreen.kt                # LoginForm, AuthenticatedView
├── search/
│   ├── SearchEvent.kt               # Search(query), LoadMore
│   ├── SearchState.kt               # Idle, InitialLoading, Results, Error
│   ├── SearchEffect.kt              # ScrollToTop
│   ├── SearchRepository.kt          # interface
│   ├── SearchViewModel.kt           # state.value read, copy() partial update
│   └── SearchScreen.kt              # SearchBar, ResultsList, load-more button
├── cart/
│   ├── CartModels.kt                # Product, CartItem
│   ├── CartEvent.kt                 # AddItem, RemoveItem, UpdateQuantity, Checkout
│   ├── CartState.kt                 # data class with derived totals
│   ├── CartEffect.kt                # OrderPlacedSuccessfully, ShowError
│   ├── CheckoutService.kt           # interface
│   ├── CartViewModel.kt             # recalculateTotals(), event delegation
│   └── CartScreen.kt                # Catalogue, CartItemRow, OrderSummary
└── fake/
    ├── FakeAuthRepository.kt
    ├── FakeSearchRepository.kt
    └── FakeCheckoutService.kt
```
