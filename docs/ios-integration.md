---
type: Guide
title: iOS integration
description: Observe state and effects from Swift, with watch under Objective-C export or AsyncSequence under Swift export, and manage the ViewModel lifecycle.
tags: [ios, swift, swiftui, uikit, lifecycle, swift-export]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T22:05:00Z }
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
  - id: swift-export-docs
    resource: https://kotlinlang.org/docs/native-swift-export.html
    title: Kotlin docs, Interoperability with Swift using Swift export
  - id: plain-flows
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonMain/kotlin/com/umain/revolver/flow/PlainFlows.kt
    title: PlainFlows.kt
---

# iOS integration

How Swift sees a ViewModel depends on how your shared module builds its iOS framework:

| Framework build | Observe with | Section |
|---|---|---|
| Objective-C export (`binaries.framework`, the default) | `state` / `effect` and `watch` | [Observing a flow](#observing-a-flow) |
| Swift export (`swiftExport { }`, Kotlin 2.4+, Alpha) | `stateFlow` / `effectFlow` as `AsyncSequence` | [Swift export](#swift-export) |

With Objective-C export, Kotlin `Flow` can't be collected directly from Swift. `state` and `effect` are wrappers (`CStateFlow`, `CSharedFlow`, both subclasses of `CFlow`) that add a `watch(onNext:)` method.

> **Property names:** `Emitter` also has `state` and `effect` properties. The Objective-C exporter renames one of each clashing pair by appending `_`. Depending on your framework setup (for example, whether it uses `export(...)` for Revolver), Swift may see the ViewModel's flows as `state_` and `effect_`. Check your generated header. The examples below use the unrenamed names.

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

## Swift export

[Swift export](https://kotlinlang.org/docs/native-swift-export.html) bridges `kotlinx.coroutines` flows to Swift's `AsyncSequence`.[^swift-export-docs] It does not bridge the `CStateFlow` and `CSharedFlow` wrapper classes. Swift export maps them to plain classes, so `state` and `effect` are not `AsyncSequence`s. Use `stateFlow` and `effectFlow` instead. They are the same streams, typed as plain `StateFlow` and `SharedFlow`.[^plain-flows]

```swift
import Shared // your Swift export module

@MainActor
final class CounterModel: ObservableObject {
    @Published private(set) var count: Int32 = 0
    private let viewModel = CounterViewModel()
    private var tasks: [Task<Void, Never>] = []

    func start() {
        tasks.append(Task { [weak self, viewModel] in
            do {
                for try await state in viewModel.stateFlow.asAsyncSequence() {
                    if let idle = state as? CounterState.Idle { self?.count = idle.count }
                }
            } catch {} // CancellationError when the task is cancelled
        })
        tasks.append(Task { [viewModel] in
            do {
                for try await effect in viewModel.effectFlow.asAsyncSequence() {
                    if effect is CounterEffect.ResetConfirmed { /* show a toast */ }
                }
            } catch {}
        })
    }

    func increment() { viewModel.emit(event: CounterEvent.Increment.shared) }

    func stop() {
        tasks.forEach { $0.cancel() }
        viewModel.dispose()
    }
}
```

Points to know:

- **Generics are erased.** Swift export erases generic parameters to their upper bound, so `stateFlow` yields `any RevolverState` and `effectFlow` yields `any RevolverEffect`. Cast to your concrete types, as above.
- **Use the concrete ViewModel class.** Swift export doesn't carry conformance to the generic `RevolverViewModelInterface` over to Swift, so call `stateFlow` on your `RevolverViewModel` subclass.
- **Cancel the task to stop observing.** Cancelling the `Task` ends iteration with a `CancellationError`. Call `dispose()` too, to cancel the ViewModel's scope. Capture `self` weakly in the task, so the model isn't kept alive by its own observation.
- **Swift export is Alpha** and works only with [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html). `watch` is also exported, but it delivers untyped values, so prefer `stateFlow` and `effectFlow`.

## Summary

| Task | Swift |
|---|---|
| Observe state | `viewModel.state.watch { }` |
| Observe effects | `viewModel.effect.watch { }` |
| Send an event | `viewModel.emit(event: MyEvent.Action.shared)` |
| Stop observing | `handle.dispose()` |
| Combine handles | `handle1 + handle2` |
| Release the ViewModel | `viewModel.dispose()` in `deinit` |
| Iterate state (Swift export) | `for try await s in viewModel.stateFlow.asAsyncSequence()` |
| Iterate effects (Swift export) | `for try await e in viewModel.effectFlow.asAsyncSequence()` |

[^ios-cflow]: iOS CFlow with watch()
[^ios-disposable]: iOS DisposableHandle
[^ios-base-viewmodel]: iOS BaseViewModel
[^swift-export-docs]: Kotlin docs, Interoperability with Swift using Swift export
[^plain-flows]: PlainFlows.kt
