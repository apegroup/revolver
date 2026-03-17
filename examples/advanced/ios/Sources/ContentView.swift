import SwiftUI
import shared // Replace with your KMP framework module name

/// Root view for the advanced example.
///
/// Renders one of three states emitted by `MainObservableViewModel` and overlays
/// a self-dismissing toast banner when a `ShowToast` effect arrives.
struct ContentView: View {

    @StateObject private var viewModel = MainObservableViewModel()

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                stateContent
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                ToastBanner(message: $viewModel.toastMessage)
            }
            .navigationTitle("Revolver — Advanced")
        }
    }

    // MARK: - State-driven content

    @ViewBuilder
    private var stateContent: some View {
        switch viewModel.state {
        case is MainViewState.Loading:
            ProgressView("Loading items…")

        case let loaded as MainViewState.Loaded:
            LoadedView(items: loaded.items, onRefresh: viewModel.refresh)

        case let error as MainViewState.Error:
            ErrorView(
                message: error.message,
                retryable: error.retryable,
                onRetry: viewModel.retry
            )

        default:
            EmptyView()
        }
    }
}

// MARK: - Sub-views

private struct LoadedView: View {
    let items: [String]
    let onRefresh: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            List(items, id: \.self) { item in
                Label(item, systemImage: "checkmark.circle")
            }
            Button("Refresh", action: onRefresh)
                .buttonStyle(.borderedProminent)
                .padding()
        }
    }
}

private struct ErrorView: View {
    let message: String
    let retryable: Bool
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 52))
                .foregroundStyle(.red)

            Text(message)
                .foregroundStyle(.red)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            if retryable {
                Button("Retry", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }
}

/// Adaptive toast banner that auto-dismisses after 2.5 seconds.
///
/// Bind to a `@Published var toastMessage: String?` on the ViewModel.
/// The view resets the binding to `nil` after the timer fires.
private struct ToastBanner: View {
    @Binding var message: String?

    var body: some View {
        if let text = message {
            Text(text)
                .font(.subheadline)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, 36)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                        withAnimation { message = nil }
                    }
                }
        }
    }
}
