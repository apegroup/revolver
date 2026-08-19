import SwiftUI

/// A self-dismissing toast banner anchored to the bottom of its parent view.
///
/// Bind `message` to a `@Published var toastMessage: String?` on any ViewModel.
/// The banner auto-dismisses after `duration` seconds and resets the binding to `nil`.
///
/// Usage:
/// ```swift
/// ZStack(alignment: .bottom) {
///     mainContent
///     ToastBanner(message: $viewModel.toastMessage)
/// }
/// ```
struct ToastBanner: View {
    @Binding var message: String?
    var duration: TimeInterval = 2.5

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
                    DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                        withAnimation { message = nil }
                    }
                }
        }
    }
}
