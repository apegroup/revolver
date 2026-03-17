---
layout: default
title: iOS Integration
nav_order: 6
description: "Observing Kotlin flows from Swift using CStateFlow, CSharedFlow, and DisposableHandle."
---

# iOS Integration

Revolver targets iOS via Kotlin Multiplatform. The shared library compiles to a native framework that can be consumed from Swift. This guide covers the Swift-side integration patterns.

---

## How flows are exposed

Kotlin's `Flow` API is not directly callable from Swift. Revolver wraps flows in platform-aware classes that expose a `watch(onNext:)` method instead:

| Kotlin | Swift-callable wrapper | Used for |
|---|---|---|
| `StateFlow<STATE>` | `CStateFlow<STATE>` | `viewModel.state` |
| `SharedFlow<EFFECT>` | `CSharedFlow<EFFECT>` | `viewModel.effect` |
| `Flow<T>` | `CFlow<T>` | General-purpose use |

---

## `watch(onNext:)`

The primary observation API. Returns a `DisposableHandle` that **must** be cancelled when the observer is deallocated:

```swift
let handle = viewModel.state.watch { state in
    // called on the main thread for each emitted value
    self.render(state: state)
}

// When done:
handle.dispose()
```

Values are delivered on the **main thread** (`Dispatchers.Main`).

---

## `DisposableHandle`

A lightweight cancellation token for a running flow observation.

```swift
// Cancel a single subscription
handle.dispose()

// Combine multiple handles with + operator
let combinedHandle = stateHandle + effectHandle
combinedHandle.dispose() // cancels both
```

Always cancel in `deinit` to avoid keeping the ViewModel's coroutine scope alive longer than needed.

---

## Lifecycle management

iOS has no automatic ViewModel clearing equivalent to Android's `onCleared()`. You must call `viewModel.dispose()` manually.

```swift
deinit {
    handle?.dispose()
    viewModel.dispose()
}
```

`dispose()` cancels the `viewModelScope` and all coroutines running within it.

---

## Recommended pattern: `ObservableObject` wrapper

Wrap the Kotlin ViewModel in a Swift `ObservableObject` to drive SwiftUI views reactively:

```swift
import SwiftUI
import shared // your KMP framework module name

final class SearchObservableViewModel: ObservableObject {

    @Published var state: SearchState = SearchState.Idle()

    private let viewModel: SearchViewModel
    private var handle: DisposableHandle?

    init(repository: SearchRepository) {
        self.viewModel = SearchViewModel(repository: repository)

        handle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }
    }

    func search(query: String) {
        viewModel.emit(event: SearchEvent.QueryChanged(query: query))
    }

    func clear() {
        viewModel.emit(event: SearchEvent.ClearResults())
    }

    deinit {
        handle?.dispose()
        viewModel.dispose()
    }
}
```

Use it in a SwiftUI view:

```swift
struct SearchView: View {
    @StateObject private var viewModel = SearchObservableViewModel(
        repository: SearchRepositoryImpl()
    )

    var body: some View {
        switch viewModel.state {
        case let loaded as SearchState.Results:
            List(loaded.items, id: \.self) { Text($0) }
        case is SearchState.Loading:
            ProgressView()
        case let error as SearchState.Error:
            Text(error.message)
        default:
            EmptyView()
        }
    }
}
```

---

## Observing effects

Observe `viewModel.effect` separately. One-shot effects (navigation, alerts) should not live in `@Published` state:

```swift
final class SearchObservableViewModel: ObservableObject {

    @Published var state: SearchState = SearchState.Idle()
    var onNavigate: ((String) -> Void)?

    private var handles: DisposableHandle?

    init(viewModel: SearchViewModel) {
        let stateHandle = viewModel.state.watch { [weak self] in
            self?.state = $0
        }

        let effectHandle = viewModel.effect.watch { [weak self] effect in
            switch effect {
            case let nav as SearchEffect.NavigateToDetail:
                self?.onNavigate?(nav.id)
            default:
                break
            }
        }

        handles = stateHandle + effectHandle
    }

    deinit {
        handles?.dispose()
    }
}
```

---

## UIKit pattern

For UIKit instead of SwiftUI, observe in `viewDidLoad` and update in the `onNext` closure:

```swift
class SearchViewController: UIViewController {

    private let observableViewModel = SearchObservableViewModel(...)
    private var cancellable: AnyCancellable?

    override func viewDidLoad() {
        super.viewDidLoad()

        cancellable = observableViewModel.$state.sink { [weak self] state in
            self?.render(state: state)
        }
    }

    private func render(state: SearchState) {
        switch state {
        case is SearchState.Loading:
            activityIndicator.startAnimating()
        case let results as SearchState.Results:
            activityIndicator.stopAnimating()
            tableView.reloadData()
        default:
            break
        }
    }
}
```

Alternatively, use `watch` directly without Combine:

```swift
private var stateHandle: DisposableHandle?

override func viewDidLoad() {
    super.viewDidLoad()
    stateHandle = viewModel.state.watch { [weak self] state in
        DispatchQueue.main.async { self?.render(state: state) }
    }
}

deinit {
    stateHandle?.dispose()
    viewModel.dispose()
}
```

> `watch` already delivers on the main thread, but wrapping in `DispatchQueue.main.async` is safe and harmless.

---

## Summary

| Task | Swift API |
|---|---|
| Observe state | `viewModel.state.watch { }` |
| Observe effects | `viewModel.effect.watch { }` |
| Send an event | `viewModel.emit(event: MyEvent.Action())` |
| Cancel a subscription | `handle.dispose()` |
| Combine handles | `let h = handle1 + handle2` |
| Clean up ViewModel | `viewModel.dispose()` (in `deinit`) |
