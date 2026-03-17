# Advanced iOS Integration Example

SwiftUI app that mirrors the advanced Android example — a list-loading screen with a
deliberate first-load failure, retry flow, and a `ShowToast` effect.

## What it shows

| Pattern | Swift implementation |
|---|---|
| Multiple events | `MainViewEvent.LoadItems()`, `MainViewEvent.RetryAfterError()` |
| Chained state transitions | `Loading → Loaded` or throws `→ Error` |
| One-shot effect bridging | `ShowToast` → `@Published var toastMessage: String?` → `ToastBanner` overlay |
| `retryable` flag | `MainViewState.Error.retryable` controls whether the Retry button is shown |
| `DisposableHandle` combining | `stateHandle + effectHandle` stored as one `handle` |

## Setup

Follow the same steps as the [basic iOS example](../../basic/ios/README.md):

1. Build the XCFramework: `./gradlew :revolver:assembleXCFramework`
2. Create an Xcode project (iOS App / SwiftUI / Swift)
3. Copy `Sources/` into the project
4. Embed `revolver.xcframework` under **Frameworks, Libraries, and Embedded Content**
5. Replace `import shared` with `import revolver` (or your module name)

## Flow walkthrough

1. App launches → `LoadItems` emitted → **Loading** spinner shown
2. After 1.5 s → Kotlin throws → `ItemErrorHandler` emits `Error("Failed to load…", retryable: true)`
3. User taps **Retry** → `RetryAfterError` emitted → **Loading** again → **Loaded** list + `ShowToast` banner
4. User taps **Refresh** → `LoadItems` again → succeeds immediately

## Key files

```
Sources/
├── RevolverExampleApp.swift          # @main entry point
├── ContentView.swift                 # NavigationStack, state switch, ToastBanner overlay
└── MainObservableViewModel.swift     # ObservableObject wrapper
                                      #   • watch() on state + effect flows
                                      #   • ShowToast → @Published toastMessage
                                      #   • retry() / refresh() intent forwarders
```

## Effect bridging pattern

```swift
// In MainObservableViewModel:
let effectHandle = viewModel.effect.watch { [weak self] effect in
    switch effect {
    case let toast as MainViewEffect.ShowToast:
        self?.toastMessage = toast.message   // @Published → view re-renders
    default:
        break
    }
}

// In ContentView — self-dismissing overlay, no dependencies:
ToastBanner(message: $viewModel.toastMessage)
```
