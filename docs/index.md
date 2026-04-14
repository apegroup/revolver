---
layout: default
title: Home
nav_order: 1
description: "Revolver: Immutable event-based state management framework for Kotlin Multiplatform."
---

# Revolver

Immutable event-based state management framework for Kotlin Multiplatform.

Revolver is a lightweight, predictable state management library for Kotlin Multiplatform (KMP). It enforces a unidirectional data flow with a single immutable state, making your UI logic easier to reason about, test, and share across Android, iOS, and beyond.

## 🚀 Key Features

- **Single Source of Truth:** One immutable state per ViewModel.
- **Event-Driven:** Clear separation between UI actions (Events) and business logic.
- **Side Effects Handling:** Dedicated stream for one-time effects (navigation, toasts, etc.).
- **Built-in Error Handling:** Seamlessly map exceptions to UI states.
- **Platform Agnostic:** Core logic stays in `commonMain`, with native integrations for Android and iOS.
- **Testable:** Designed for easy unit testing of the entire state machine.

## 📦 Installation

Revolver is hosted on GitHub Packages.

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

## 📄 More Guides

- [Error Handling](error-handling.md)
