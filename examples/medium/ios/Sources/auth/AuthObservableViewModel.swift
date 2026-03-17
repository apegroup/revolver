import Foundation
import shared // Replace with your KMP framework module name

/// SwiftUI-compatible wrapper around the Kotlin `AuthViewModel`.
///
/// Bridges login/logout events to the Kotlin ViewModel and exposes state and
/// one-shot effects as `@Published` properties for SwiftUI consumption.
final class AuthObservableViewModel: ObservableObject {

    // MARK: - Published state

    @Published private(set) var state: AuthState = AuthState.Idle()

    /// Non-nil while a toast should be displayed. The view resets it to `nil` on dismissal.
    @Published var toastMessage: String? = nil

    // MARK: - Private

    private let viewModel: AuthViewModel
    private var handle: (any DisposableHandle)?

    // MARK: - Init / deinit

    init(repository: any AuthRepository) {
        self.viewModel = AuthViewModel(repository: repository)

        let stateHandle = viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }

        let effectHandle = viewModel.effect.watch { [weak self] effect in
            switch effect {
            case let toast as AuthEffect.ShowToast:
                self?.toastMessage = toast.text
            case is AuthEffect.NavigateToDashboard:
                // No-op in this example — handled by parent coordinator in a real app.
                break
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

    func login(username: String, password: String) {
        viewModel.emit(event: AuthEvent.Login(username: username, password: password))
    }

    func logout() {
        viewModel.emit(event: AuthEvent.Logout())
    }
}
