import SwiftUI

/// App entry point.
///
/// Replace `ContentView` with your own root view.
/// The `MainObservableViewModel` is created as a `@StateObject` inside `ContentView`
/// so SwiftUI manages its lifetime alongside the view.
@main
struct RevolverExampleApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
