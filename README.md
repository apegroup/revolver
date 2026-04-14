<p align="center"><img src="https://raw.githubusercontent.com/apegroup/revolver/main/assets/logo.svg" width="150"/></p>
<h1 align="center"> Revolver</h1>
<p align="center"> Immutable event-based state management framework for Kotlin Multiplatform.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.3.20--RC3-blue.svg?style=flat&logo=kotlin" alt="Kotlin version">
  <img src="https://img.shields.io/badge/Platform-KMP-orange.svg?style=flat" alt="Platform KMP">
</p>

Revolver is a lightweight, predictable state management library for Kotlin Multiplatform (KMP). It enforces a unidirectional data flow with a single immutable state, making your UI logic easier to reason about, test, and share across Android, iOS, and beyond.

---

## 🚀 Key Features

- **Single Source of Truth:** One immutable state per ViewModel.
- **Event-Driven:** Clear separation between UI actions (Events) and business logic.
- **Side Effects Handling:** Dedicated stream for one-time effects (navigation, toasts, etc.).
- **Built-in Error Handling:** Seamlessly map exceptions to UI states.
- **Platform Agnostic:** Core logic stays in `commonMain`, with native integrations for Android and iOS.
- **Testable:** Designed for easy unit testing of the entire state machine.

---

## 📦 Installation

Revolver is hosted on GitHub Packages. Add the following to your `settings.gradle.kts` or `build.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/apegroup/revolver/")
            credentials {
                username = System.getenv("GH_USERNAME") ?: ""
                password = System.getenv("GH_TOKEN") ?: ""
            }
        }
    }
}
```

Then, add the dependency to your `commonMain` source set:

```kotlin
implementation("com.umain:revolver:{LATEST_VERSION}")
```

---

## 🛠️ How it Works

Revolver follows a simple **Event -> ViewModel -> State/Effect** flow.

### 1. Define your Contract

```kotlin
// 1. Events (User/UI actions)
sealed class MainEvent : RevolverEvent {
    object Refresh : MainEvent()
}

// 2. State (What the UI shows)
sealed class MainState : RevolverState {
    object Loading : MainState()
    data class Success(val data: String) : MainState()
    data class Error(val message: String) : MainState()
}

// 3. Effects (One-time actions)
sealed class MainEffect : RevolverEffect {
    data class ShowToast(val text: String) : MainEffect()
}
```

### 2. Implement the ViewModel

```kotlin
class MainViewModel : RevolverViewModel<MainEvent, MainState, MainEffect>(
    initialState = MainState.Loading
) {
    init {
        addEventHandler<MainEvent.Refresh>(::onRefresh)
        addErrorHandler<Exception> { _, emit -> emit.state(MainState.Error("Something went wrong")) }
    }

    private suspend fun onRefresh(event: MainEvent.Refresh, emit: Emitter<MainState, MainEffect>) {
        emit.state(MainState.Loading)
        val result = fetchData() // Your logic here
        emit.state(MainState.Success(result))
    }
}
```

---

## 📱 Platform Usage

### Android
Revolver integrates directly with `androidx.lifecycle.ViewModel`. Use it as you would any other ViewModel.

### iOS (Swift)
Revolver provides `CFlow` wrappers to make observing states and effects easy from Swift.

```swift
let viewModel = MainViewModel()

// Observe state
viewModel.state.watch { state in
    // Update UI
}

// Observe effects
viewModel.effect.watch { effect in
    // Handle navigation, toasts, etc.
}
```

---

## 🧪 Testing

Revolver's architecture allows you to test your entire business logic in `commonTest` without any platform dependencies.

```kotlin
@Test
fun `refresh should emit Loading then Success`() = runTest {
    val viewModel = MainViewModel()
    viewModel.state.test {
        viewModel.emit(MainEvent.Refresh)
        
        assertIs<MainState.Loading>(awaitItem())
        val success = assertIs<MainState.Success>(awaitItem())
        assertEquals("Data", success.data)
    }
}
```

---

## 📄 Documentation

For more detailed guides, check the [docs](docs/) folder:
- [Error Handling Deep Dive](docs/error-handling.md)
- [Testing Guide](docs/testing.md)
- [Swift Integration](docs/ios-swift-integration.md)

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue.

---

## ⚖️ License

Revolver is released under the MIT License. See [LICENSE](LICENSE) for details.
