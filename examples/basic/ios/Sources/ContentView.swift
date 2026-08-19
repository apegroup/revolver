import SwiftUI
import shared // Replace with your KMP framework module name

/// Root view driven by `MainObservableViewModel`.
///
/// Renders one of three states emitted by the Kotlin ViewModel:
/// - **Loading** — spinner while work is in progress
/// - **Loaded** — welcome message
/// - **Error** — error text in red
struct ContentView: View {

    @StateObject private var viewModel = MainObservableViewModel()

    var body: some View {
        switch viewModel.state {
        case is MainViewState.Loading:
            ProgressView("Loading…")

        case let loaded as MainViewState.Loaded:
            VStack(spacing: 16) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.green)
                Text(loaded.welcomeMessage)
                    .font(.title3)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

        case let error as MainViewState.Error:
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.red)
                Text(error.errorMessage)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

        default:
            EmptyView()
        }
    }
}
