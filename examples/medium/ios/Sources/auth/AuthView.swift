import SwiftUI
import shared // Replace with your KMP framework module name

/// Auth screen: login form, loading indicator, authenticated welcome, and error state —
/// driven entirely by `AuthObservableViewModel`.
struct AuthView: View {

    @ObservedObject var viewModel: AuthObservableViewModel

    var body: some View {
        ZStack(alignment: .bottom) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            ToastBanner(message: $viewModel.toastMessage)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case is AuthState.Authenticating:
            ProgressView("Authenticating…")

        case let authenticated as AuthState.Authenticated:
            AuthenticatedView(
                userName: authenticated.userName,
                onLogout: viewModel.logout
            )

        case let error as AuthState.Error:
            LoginForm(errorMessage: error.message, onLogin: viewModel.login)

        default: // Idle
            LoginForm(errorMessage: nil, onLogin: viewModel.login)
        }
    }
}

// MARK: - Sub-views

private struct LoginForm: View {
    let errorMessage: String?
    let onLogin: (String, String) -> Void

    @State private var username = ""
    @State private var password = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("Sign In")
                .font(.largeTitle.bold())

            if let error = errorMessage {
                Text(error)
                    .foregroundStyle(.red)
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
            }

            TextField("Username", text: $username)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)

            Button("Log In") { onLogin(username, password) }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)

            Text("Hint: any username/password works. Password \"fail\" triggers an error.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(24)
    }
}

private struct AuthenticatedView: View {
    let userName: String
    let onLogout: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Text("Welcome, \(userName) 👋")
                .font(.title.bold())

            Button("Log Out", action: onLogout)
                .buttonStyle(.bordered)
        }
    }
}
