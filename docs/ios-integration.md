---
type: Guide
title: iOS integration
description: Observe state and effects from Swift with watch, and manage the ViewModel lifecycle.
tags: [ios, swift, swiftui, uikit, lifecycle]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T21:23:43Z }
sources:
  - id: ios-cflow
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/iosMain/kotlin/com/umain/revolver/flow/CFlow.kt
    title: iOS CFlow with watch()
  - id: ios-disposable
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/iosMain/kotlin/com/umain/revolver/flow/DisposableHandle.kt
    title: iOS DisposableHandle
  - id: ios-base-viewmodel
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/iosMain/kotlin/com/umain/revolver/BaseViewModel.kt
    title: iOS BaseViewModel
---

# iOS integration

Kotlin `Flow` can't be collected directly from Swift. On iOS, `state` and `effect` are wrappers (`CStateFlow`, `CSharedFlow`, both subclasses of `CFlow`) that add a `watch(onNext:)` method.

## Observing a flow

`watch` starts collecting on `Dispatchers.Main` and returns a `DisposableHandle`.[^ios-cflow] Call `dispose()` on the handle to stop observing:

```swift
let handle = viewModel.state.watch { state in
    render(state)
}

handle.dispose()
```

Combine handles with `+` to dispose several at once:[^ios-disposable]

```swift
let handles = stateHandle + effectHandle
handles.dispose()
```

## Lifecycle

On Android the ViewModel scope is cancelled in `onCleared()`. iOS has no equivalent, so call `viewModel.dispose()` when the owner goes away. Otherwise the scope and its coroutines keep running.[^ios-base-viewmodel]

## SwiftUI: an `ObservableObject` wrapper

```swift
import SwiftUI
import shared // your KMP framework

final class SearchObservableViewModel: ObservableObject {
    @Published private(set) var state: SearchState = SearchState.Idle.shared
    var onNavigate: ((String) -> Void)?

    private let viewModel: SearchViewModel
    private var handles: DisposableHandle?

    init(repository: SearchRepository) {
        viewModel = SearchViewModel(repository: repository)

        let stateHandle = viewModel.state.watch { [weak self] state in
            self?.state = state
        }
        let effectHandle = viewModel.effect.watch { [weak self] effect in
            if let nav = effect as? SearchEffect.NavigateToDetail {
                self?.onNavigate?(nav.id)
            }
        }
        handles = stateHandle + effectHandle
    }

    func search(_ query: String) {
        viewModel.emit(event: SearchEvent.QueryChanged(query: query))
    }

    deinit {
        handles?.dispose()
        viewModel.dispose()
    }
}

struct SearchView: View {
    @StateObject private var model = SearchObservableViewModel(repository: SearchRepositoryImpl())

    var body: some View {
        switch model.state {
        case is SearchState.Loading:
            ProgressView()
        case let results as SearchState.Results:
            List(results.items, id: \.self) { Text($0) }
        case let error as SearchState.Error:
            Text(error.message)
        default:
            EmptyView()
        }
    }
}
```

Kotlin `object` declarations are accessed through `.shared` in Swift, for example `SearchState.Idle.shared`. Keep effects out of `@Published` properties so they are handled once and not re-rendered.

## UIKit

Observe in `viewDidLoad`. `watch` already delivers on the main thread:

```swift
final class SearchViewController: UIViewController {
    private let viewModel = SearchViewModel(repository: SearchRepositoryImpl())
    private var stateHandle: DisposableHandle?

    override func viewDidLoad() {
        super.viewDidLoad()
        stateHandle = viewModel.state.watch { [weak self] state in
            self?.render(state)
        }
    }

    deinit {
        stateHandle?.dispose()
        viewModel.dispose()
    }
}
```

## Summary

| Task | Swift |
|---|---|
| Observe state | `viewModel.state.watch { }` |
| Observe effects | `viewModel.effect.watch { }` |
| Send an event | `viewModel.emit(event: MyEvent.Action.shared)` |
| Stop observing | `handle.dispose()` |
| Combine handles | `handle1 + handle2` |
| Release the ViewModel | `viewModel.dispose()` in `deinit` |

[^ios-cflow]: iOS CFlow with watch()
[^ios-disposable]: iOS DisposableHandle
[^ios-base-viewmodel]: iOS BaseViewModel
