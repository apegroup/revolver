import Foundation
import shared // Replace with your KMP framework module name

/// SwiftUI-compatible wrapper around the Kotlin `MainViewModel` from the advanced example.
///
/// Extends the basic example's wrapper to handle:
/// - Multiple events (`LoadItems`, `RetryAfterError`)
/// - A one-shot `ShowToast` effect bridged via `@Published var toastMessage`
/// - The `retryable` flag on `MainViewState.Error` exposed to the view
///
/// ## Lifecycle
/// Create via `@StateObject` so SwiftUI owns and retains it for the lifetime of the screen.
final class MainObservableViewModel: ObservableObject {

    // MARK: - Published state

    /// Mirrors `MainViewModel.state`. Updated on the main thread for every emission.
    @Published private(set) var state: MainViewState = MainViewState.Loading()

    /// Non-nil while a toast is visible. The view sets it back to `nil` after dismissal.
    @Published var toastMessage: String? = nil

    // MARK: - Private

    private let viewModel: MainViewModel
    private var handle: (any DisposableHandle)?

    // MARK: - Init / deinit

    init() {
        self.viewModel = MainViewModel()

        let stateHandle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }

        let effectHandle = viewModel.effect.watch { [weak self] effect in
            switch effect {
            case let toast as MainViewEffect.ShowToast:
                // Bridge the one-shot Kotlin effect to a @Published property.
                self?.toastMessage = toast.message
            default:
                break
            }
        }

        handle = stateHandle + effectHandle

        // Kick off the initial load — this will intentionally fail once to show
        // the Error state + Retry flow.
        viewModel.emit(event: MainViewEvent.LoadItems())
    }

    deinit {
        handle?.dispose()
        viewModel.dispose()
    }

    // MARK: - Intent forwarding

    /// Retry after a failed load.
    func retry() {
        viewModel.emit(event: MainViewEvent.RetryAfterError())
    }

    /// Reload the list from scratch.
    func refresh() {
        viewModel.emit(event: MainViewEvent.LoadItems())
    }
}
