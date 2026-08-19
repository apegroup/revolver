import Foundation
import shared // Replace with your KMP framework module name

/// SwiftUI-compatible wrapper around the Kotlin `SearchViewModel`.
///
/// Key patterns demonstrated:
/// - `ScrollToTop` effect bridged via a `scrollTrigger` counter — the view observes
///   it with `onChange` and calls `scrollProxy.scrollTo(0)`.
/// - `LoadMore` idempotency: the Kotlin ViewModel already guards against duplicate
///   calls; the Swift wrapper just forwards the event.
final class SearchObservableViewModel: ObservableObject {

    // MARK: - Published state

    @Published private(set) var state: SearchState = SearchState.Idle()

    /// Incremented each time a `ScrollToTop` effect fires.
    /// Views observe this with `onChange(of: scrollTrigger)` to animate back to item 0.
    @Published private(set) var scrollTrigger: Int = 0

    // MARK: - Private

    private let viewModel: SearchViewModel
    private var handle: (any DisposableHandle)?

    // MARK: - Init / deinit

    init(repository: any SearchRepository) {
        self.viewModel = SearchViewModel(repository: repository)

        let stateHandle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }

        let effectHandle = viewModel.effect.watch { [weak self] effect in
            switch effect {
            case is SearchEffect.ScrollToTop:
                // Counter-based trigger: safe to observe with onChange even if
                // the value hasn't changed between navigations.
                self?.scrollTrigger += 1
            default:
                break
            }
        }

        handle = stateHandle + effectHandle
    }

    deinit {
        handle?.dispose()
        viewModel.dispose()
    }

    // MARK: - Intent forwarding

    func search(query: String) {
        viewModel.emit(event: SearchEvent.Search(query: query))
    }

    func loadMore() {
        viewModel.emit(event: SearchEvent.LoadMore())
    }
}
