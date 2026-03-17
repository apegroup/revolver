import Foundation
import shared // Replace with your KMP framework module name

// MARK: - FakeAuthRepository

/// In-memory `AuthRepository` for the iOS example.
///
/// Accepts any non-blank credentials. Simulates a 1-second network round-trip.
/// Password `"fail"` throws to demonstrate the Error state.
final class FakeAuthRepository: AuthRepository {

    func login(username: String, password: String) async throws -> String {
        try await Task.sleep(nanoseconds: 1_000_000_000)
        if password == "fail" { throw NSError(domain: "Auth", code: 0, userInfo: [NSLocalizedDescriptionKey: "Invalid credentials."]) }
        return username.prefix(1).uppercased() + username.dropFirst()
    }
}

// MARK: - FakeSearchRepository

/// In-memory `SearchRepository` for the iOS example.
///
/// Returns 2 pages × 10 items, then an empty list to signal end-of-results.
/// Simulates a 700 ms network delay.
final class FakeSearchRepository: SearchRepository {

    func fetchPage(query: String, page: Int32, pageSize: Int32) async throws -> [String] {
        try await Task.sleep(nanoseconds: 700_000_000)
        if page >= 2 { return [] }
        return (0..<pageSize).map { idx in
            let globalIdx = Int(page) * Int(pageSize) + Int(idx) + 1
            return "\"\(query)\" result #\(globalIdx)"
        }
    }
}

// MARK: - FakeCheckoutService

/// In-memory `CheckoutService` for the iOS example.
///
/// Always succeeds after a simulated 1.2-second processing delay.
/// Uncomment the `throw` line to test the payment-failure flow.
final class FakeCheckoutService: CheckoutService {

    func processCheckout(amount: Double) async throws {
        try await Task.sleep(nanoseconds: 1_200_000_000)
        // throw NSError(domain: "Payment", code: 0, userInfo: [NSLocalizedDescriptionKey: "Card declined."])
    }
}
