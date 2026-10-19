<p align="center"><img src="https://raw.githubusercontent.com/apegroup/revolver/main/assets/logo.svg" width="150"/></p>
<h1 align="center">Revolver</h1>
<p align="center">Immutable event-based state management for Kotlin Multiplatform</p>

<br/>

## Overview

Revolver is a Kotlin Multiplatform state management library that enforces a single immutable state and unidirectional data flow. Clients send **Events** to a **ViewModel**, which processes them and emits **States** and **Effects** back via Kotlin Flows.

```
Client ──emit(Event)──► RevolverViewModel ──► EventHandler
                                                    │
                              ┌─────────────────────┤
                              ▼                     ▼
                        StateFlow<State>    SharedFlow<Effect>
                              │                     │
                              └──────────► Clients ◄┘
```

| Type | Direction | Purpose |
|---|---|---|
| `RevolverEvent` | Client → ViewModel | User actions or lifecycle triggers |
| `RevolverState` | ViewModel → Client | Immutable snapshot of what to display |
| `RevolverEffect` | ViewModel → Client | One-shot side effect (navigation, toast, etc.) |

<br/>

## Installation

Add the GitHub Packages Maven repository to your `settings.gradle.kts`:

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

Then declare the dependency in your `commonMain` source set:

```kotlin
implementation("com.umain:revolver:1.6.0")
```

<br/>

## Quick Start

### 1. Define your sealed classes

```kotlin
sealed class ExampleEvent : RevolverEvent {
    object Refresh : ExampleEvent()
}

sealed class ExampleState : RevolverState {
    object Loading : ExampleState()
    data class Loaded(val result: String) : ExampleState()
    data class Error(val message: String) : ExampleState()
}

sealed class ExampleEffect : RevolverEffect {
    data class ShowToast(val message: String) : ExampleEffect()
}
```

> Prefer `object` for states with no data and `data class` for states that carry data. The state flow skips values equal to the current one, so structural equality avoids redundant updates.

### 2. Implement your ViewModel

```kotlin
class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(
    initialState = ExampleState.Loading,
) {

    init {
        addEventHandler<ExampleEvent.Refresh>(::onRefresh)
        addErrorHandler(RevolverDefaultErrorHandler(ExampleState.Error("Something went wrong")))
    }

    private suspend fun onRefresh(
        event: ExampleEvent.Refresh,
        emit: Emitter<ExampleState, ExampleEffect>,
    ) {
        emit.state(ExampleState.Loading)
        val data = fetchData()
        emit.state(ExampleState.Loaded(data))
        emit.effect(ExampleEffect.ShowToast("Loaded!"))
    }
}
```

### 3. Collect state and effects (Android — Jetpack Compose)

```kotlin
@Composable
fun ExampleScreen(viewModel: ExampleViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.emit(ExampleEvent.Refresh)
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExampleEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val s = state) {
        is ExampleState.Loading -> CircularProgressIndicator()
        is ExampleState.Loaded  -> Text(s.result)
        is ExampleState.Error   -> Text(s.message)
    }
}
```

For iOS, see [iOS integration](docs/ios-integration.md).

<br/>

## Documentation

The [`docs/`](docs/index.md) directory is an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) bundle: plain markdown with YAML frontmatter that people and AI agents can both read.

| Page | Covers |
|---|---|
| [Getting started](docs/getting-started.md) | Installation, platform support, first ViewModel |
| [Core concepts](docs/concepts.md) | Event, State, Effect, Emitter, and runtime behaviour |
| [Error handling](docs/error-handling.md) | Built-in, per-type and reusable error handlers |
| [Testing](docs/testing.md) | Testing ViewModels with Turbine and kotlinx-coroutines-test |
| [iOS integration](docs/ios-integration.md) | `watch`, `DisposableHandle`, SwiftUI and UIKit patterns |
| [API reference](docs/api-reference.md) | Every public type and function |

<br/>

## Contribution

Bug reports, feature requests, and pull requests are welcome. This library is in active development — production use is at your own discretion.

Run the test suite with `./scripts/run_test_suite.sh`, and publish to your local Maven repository with `./scripts/build_deploy_to_local_maven.sh`.
