# Revolver - Kotlin Multiplatform State Management Framework

Revolver is an immutable, event-based state management framework built for Kotlin Multiplatform (KMP). It provides a structured way to handle UI state and side effects using a Redux-inspired, event-driven architecture centered around the `RevolverViewModel`.

## Project Overview

- **Core Architecture:** Event-driven, immutable state management.
- **Key Components:**
    - `RevolverEvent`: Sealed classes representing actions or inputs to the ViewModel.
    - `RevolverState`: Sealed classes representing the immutable UI state.
    - `RevolverEffect`: Sealed classes for one-time side effects (e.g., navigation, toasts).
    - `RevolverViewModel`: The core logic hub that maps events to state/effect updates.
- **Technology Stack:**
    - **Language:** Kotlin 2.1.0+ (KMP).
    - **Concurrency:** Kotlin Coroutines & Flow.
    - **Logging:** Napier.
    - **Testing:** Turbine, Mockative, kotlinx-coroutines-test.
- **Supported Platforms:**
    - **Android:** Integrates with `androidx.lifecycle.ViewModel`.
    - **iOS:** Custom `BaseViewModel` with a `dispose()` method for scope management.
    - **JVM:** Supported via multiplatform configuration.

## Project Structure

- `revolver/`: The core library module.
    - `src/commonMain/`: Core framework classes (`RevolverViewModel`, `Emitter`, etc.).
    - `src/androidMain/`: Android-specific implementations (e.g., `BaseViewModel` extending Android's `ViewModel`).
    - `src/iosMain/`: iOS-specific implementations (e.g., `BaseViewModel` and Flow wrappers for Swift compatibility).
- `examples/basic/`: A reference implementation demonstrating how to integrate Revolver into an Android application.
- `scripts/`: Utility scripts for common development tasks.

## Building and Running

The project uses Gradle (KTS) for building and dependency management.

### Key Commands

- **Build Project:**
  ```bash
  ./gradlew build
  ```
- **Clean Project:**
  ```bash
  ./gradlew clean
  ```
- **Run Tests:**
  Runs both Android unit tests and iOS simulator tests.
  ```bash
  ./scripts/run_test_suite.sh
  ```
- **Publish to Local Maven:**
  Publishes the library to your local Maven repository for testing in other projects.
  ```bash
  ./scripts/build_deploy_to_local_maven.sh
  ```
- **Android Specific Tests:**
  ```bash
  ./gradlew :revolver:testDebugUnitTest
  ```
- **iOS Specific Tests:**
  ```bash
  ./gradlew :revolver:iosSimulatorArm64Test
  ```

## Development Conventions

### State Management Guidelines
- **Immutability:** Always use immutable data classes for `RevolverState` and `RevolverEvent`.
- **Sealed Classes:** Use `sealed class` for Events, States, and Effects to ensure exhaustive handling.
- **Event Handlers:** Register handlers using `addEventHandler<EventType>(::handlerFunc)` in the ViewModel's `init` block.
- **Error Handling:** Every ViewModel should register at least one error handler using `addErrorHandler` to catch and map exceptions to UI states.

### Testing Practices
- Focus unit tests on the `RevolverViewModel` logic in `commonTest`.
- Use **Turbine** to assert `state` and `effect` flow emissions.
- Use **Mockative** for mocking dependencies in KMM tests.
- Ensure all tests pass on both Android and iOS targets.

### Platform Specifics
- **Android:** The `viewModelScope` is tied to the Android `ViewModel` lifecycle.
- **iOS:** iOS clients should call `dispose()` on the ViewModel when it's no longer needed to cancel the `viewModelScope`.
- **Flow Wrappers:** `CFlow`, `CStateFlow`, and `CSharedFlow` are provided to make Kotlin Flows easier to consume from Swift.
