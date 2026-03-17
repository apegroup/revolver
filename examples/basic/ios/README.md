# Basic iOS Integration Example

SwiftUI app that mirrors the basic Android example — loads a welcome message via
the shared Kotlin `MainViewModel` and renders Loading / Loaded / Error states.

## Prerequisites

- Xcode 15+
- iOS 14+ deployment target
- The Revolver library compiled to an XCFramework (see below)

## Setup

### 1. Build the XCFramework

From the repository root:

```bash
./gradlew :revolver:assembleXCFramework
```

This produces:
```
revolver/build/XCFrameworks/release/revolver.xcframework
```

### 2. Create an Xcode project

1. Open Xcode → **File → New → Project** → iOS App
2. Set **Interface** to SwiftUI, **Language** to Swift
3. Copy the files from `Sources/` into the project

### 3. Add the framework

1. In Xcode, select your target → **General → Frameworks, Libraries, and Embedded Content**
2. Tap **+** → **Add Other… → Add Files…**
3. Select `revolver.xcframework`
4. Set **Embed** to *Embed & Sign*

### 4. Update the import

In each Swift file, replace:
```swift
import shared
```
with the name of your XCFramework module (default: `revolver`):
```swift
import revolver
```

### 5. Run

Select an iOS 14+ simulator and press **Run** (⌘R).

---

## How it works

```
SwiftUI View
    └── @StateObject MainObservableViewModel
            ├── MainViewModel (Kotlin)
            │       └── emits MainViewState via CStateFlow
            └── watch(onNext:) → @Published state → view re-renders
```

### Key Swift APIs

| API | Purpose |
|---|---|
| `viewModel.state.watch { }` | Subscribe to `CStateFlow<MainViewState>` on the main thread |
| `viewModel.effect.watch { }` | Subscribe to `CSharedFlow<RevolverEffect>` for one-shot effects |
| `handle1 + handle2` | Combine `DisposableHandle` instances into one |
| `handle.dispose()` | Cancel a flow subscription |
| `viewModel.dispose()` | Cancel the Kotlin coroutine scope — call in `deinit` |

### File overview

| File | Role |
|---|---|
| `RevolverExampleApp.swift` | App entry point (`@main`) |
| `ContentView.swift` | SwiftUI view — switches on `viewModel.state` |
| `MainObservableViewModel.swift` | `ObservableObject` wrapper — owns the Kotlin ViewModel, manages handles |

---

## Extending to the advanced example

To adapt this for the advanced Android example's ViewModel (`LoadItems`, `RetryAfterError`, effects):

```swift
// In MainObservableViewModel, replace the init body:
let stateHandle = viewModel.state.watch { [weak self] state in
    self?.state = state
}

let effectHandle = viewModel.effect.watch { [weak self] effect in
    switch effect {
    case let toast as MainViewEffect.ShowToast:
        self?.toastMessage = toast.message
    default:
        break
    }
}

handle = stateHandle + effectHandle
viewModel.emit(event: MainViewEvent.LoadItems())

// Send retry:
func retry() {
    viewModel.emit(event: MainViewEvent.RetryAfterError())
}
```
