---
type: Guide
title: Getting started
description: Install Revolver, then build and connect your first ViewModel on Android and iOS.
tags: [installation, setup, android, compose, ios]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-19T12:32:31Z }
sources:
  - id: build-config
    resource: https://github.com/apegroup/revolver/blob/main/revolver/build.gradle.kts
    title: Library build configuration
  - id: gradle-properties
    resource: https://github.com/apegroup/revolver/blob/main/gradle.properties
    title: SDK levels and library version
---

# Getting started

## Installation

Revolver is published to GitHub Packages. Add the repository in `settings.gradle.kts`:

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

`GH_TOKEN` must be a GitHub personal access token with at least the `read:packages` scope.

Then add the dependency to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.umain:revolver:1.7.0")
        }
    }
}
```

## Platform support

| Target | Requirement |
|---|---|
| Android | `minSdk` 29 |
| iOS | 14.0 (`iosArm64`, `iosX64`, `iosSimulatorArm64`) |

The library is built with Kotlin 2.4 and JDK 21.[^build-config] [^gradle-properties]

## Your first ViewModel

### 1. Define events, states and effects

```kotlin
sealed class CounterEvent : RevolverEvent {
    object Increment : CounterEvent()
    object Reset : CounterEvent()
}

sealed class CounterState : RevolverState {
    data class Idle(val count: Int = 0) : CounterState()
}

sealed class CounterEffect : RevolverEffect {
    object ResetConfirmed : CounterEffect()
}
```

[Core concepts](concepts.md) covers what each of these is for.

### 2. Implement the ViewModel

```kotlin
class CounterViewModel : RevolverViewModel<CounterEvent, CounterState, CounterEffect>(
    initialState = CounterState.Idle(),
) {
    init {
        addEventHandler<CounterEvent.Increment>(::onIncrement)
        addEventHandler<CounterEvent.Reset>(::onReset)
        addErrorHandler(RevolverDefaultErrorHandler(CounterState.Idle()))
    }

    private suspend fun onIncrement(
        event: CounterEvent.Increment,
        emit: Emitter<CounterState, CounterEffect>,
    ) {
        val current = (state.value as CounterState.Idle).count
        emit.state(CounterState.Idle(current + 1))
    }

    private suspend fun onReset(
        event: CounterEvent.Reset,
        emit: Emitter<CounterState, CounterEffect>,
    ) {
        emit.state(CounterState.Idle(0))
        emit.effect(CounterEffect.ResetConfirmed)
    }
}
```

### 3. Connect it on Android (Jetpack Compose)

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CounterEffect.ResetConfirmed ->
                    Toast.makeText(context, "Counter reset", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val s = state) {
        is CounterState.Idle -> Column {
            Text("Count: ${s.count}")
            Button(onClick = { viewModel.emit(CounterEvent.Increment) }) { Text("+") }
            Button(onClick = { viewModel.emit(CounterEvent.Reset) }) { Text("Reset") }
        }
    }
}
```

Emit events from callbacks or from `LaunchedEffect`, never directly in the composable body. Code in the body runs again on every recomposition.

### 4. Connect it on iOS (SwiftUI)

Wrap the ViewModel in an `ObservableObject`, observe with `watch`, and dispose in `deinit`. [iOS integration](ios-integration.md) walks through the full pattern.

## Next steps

- [Error handling](error-handling.md): map exceptions to states and effects
- [Testing](testing.md): unit test ViewModels in `commonTest`
- [API reference](api-reference.md): every public symbol

[^build-config]: Library build configuration
[^gradle-properties]: SDK levels and library version
