import Foundation
import shared // Replace with your KMP framework module name

/// SwiftUI-compatible wrapper around the Kotlin `MainViewModel`.
///
/// Bridges the Kotlin `CStateFlow` / `CSharedFlow` APIs to SwiftUI's
/// `@Published` / `ObservableObject` system using `watch(onNext:)` and `DisposableHandle`.
///
/// ## Lifecycle
/// Create one instance per screen (e.g. via `@StateObject`) and let SwiftUI manage its
/// lifetime. The underlying Kotlin ViewModel is disposed when this object is deallocated.
final class MainObservableViewModel: ObservableObject {

    // MARK: - Published state

    /// Mirrors `MainViewModel.state`. Updated on the main thread for every emission.
    @Published private(set) var state: MainViewState = MainViewState.Loading()

    // MARK: - Private

    private let viewModel: MainViewModel
    private var handle: (any DisposableHandle)?

    // MARK: - Init / deinit

    init() {
        self.viewModel = MainViewModel()

        // Subscribe to the state flow. `watch` delivers on the main thread.
        let stateHandle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }

        // Subscribe to one-shot effects.
        let effectHandle = viewModel.effect.watch { [weak self] effect in
            self?.handleEffect(effect)
        }

        // Combine into a single handle for simpler cleanup.
        handle = stateHandle + effectHandle

        // Kick off the initial load.
        viewModel.emit(event: MainViewEvent.ViewReady(parameters: "iOS"))
    }

    deinit {
        handle?.dispose()     // Cancel flow subscriptions.
        viewModel.dispose()   // Cancel the Kotlin coroutine scope.
    }

    // MARK: - Effects

    private func handleEffect(_ effect: any RevolverEffect) {
        // No effects defined in the basic example's ViewModel.
        // In the advanced example you would switch on effect type here.
    }
}
