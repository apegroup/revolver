# Getting Started

## Installation

Revolver is published to GitHub Packages. Add the repository to your `settings.gradle.kts`:

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

Then add the dependency in your `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.umain:revolver:1.6.0")
        }
    }
}
```

> **GitHub token**: The `GH_TOKEN` environment variable must be a GitHub Personal Access Token (classic or fine-grained) with at least `read:packages` scope.

---

## Minimum requirements

| Platform | Minimum version |
|---|---|
| Android | API 29 (Android 10) |
| iOS | 14.0 |
| JVM | Java 21 |
| Kotlin | 2.3.0 |

---

## Your first ViewModel in 4 steps

### Step 1 — Define events

Events are messages sent **from the client to the ViewModel**. Model them as a sealed class:

```kotlin
sealed class CounterEvent : RevolverEvent {
    object Increment : CounterEvent()
    object Decrement : CounterEvent()
    object Reset : CounterEvent()
}
```

### Step 2 — Define states

States are **immutable snapshots** of what the UI should render. Use `object` for states without data and `data class` for states that carry a payload:

```kotlin
sealed class CounterState : RevolverState {
    data class Idle(val count: Int = 0) : CounterState()
}
```

> Avoid plain `class` for state subtypes — it bypasses structural equality and can suppress emissions when the ViewModel sets the same logical value twice.

### Step 3 — Define effects (optional)

Effects are **one-shot side actions** such as navigation or toasts. Skip this step if your ViewModel has no side effects:

```kotlin
sealed class CounterEffect : RevolverEffect {
    object ResetConfirmed : CounterEffect()
}
```

### Step 4 — Implement the ViewModel

```kotlin
class CounterViewModel : RevolverViewModel<CounterEvent, CounterState, CounterEffect>(
    initialState = CounterState.Idle(),
) {

    init {
        addEventHandler<CounterEvent.Increment>(::onIncrement)
        addEventHandler<CounterEvent.Decrement>(::onDecrement)
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

    private suspend fun onDecrement(
        event: CounterEvent.Decrement,
        emit: Emitter<CounterState, CounterEffect>,
    ) {
        val current = (state.value as CounterState.Idle).count
        emit.state(CounterState.Idle(current - 1))
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

---

## Connecting to Android (Jetpack Compose)

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.emit(CounterEvent.Increment) // trigger an initial event if needed

        viewModel.effect.collect { effect ->
            when (effect) {
                is CounterEffect.ResetConfirmed ->
                    Toast.makeText(LocalContext.current, "Counter reset!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val s = state) {
        is CounterState.Idle -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Count: ${s.count}")
                Button(onClick = { viewModel.emit(CounterEvent.Increment) }) { Text("+") }
                Button(onClick = { viewModel.emit(CounterEvent.Decrement) }) { Text("−") }
                Button(onClick = { viewModel.emit(CounterEvent.Reset) }) { Text("Reset") }
            }
        }
    }
}
```

---

## Connecting to iOS (SwiftUI)

See the full [iOS Integration guide](ios-integration.md).

```swift
class CounterObservableViewModel: ObservableObject {
    @Published var count: Int = 0

    private let viewModel = CounterViewModel()
    private var handle: DisposableHandle?

    init() {
        handle = viewModel.state.watch { [weak self] state in
            if let idle = state as? CounterState.Idle {
                self?.count = Int(idle.count)
            }
        }
    }

    func increment() { viewModel.emit(event: CounterEvent.Increment()) }
    func decrement() { viewModel.emit(event: CounterEvent.Decrement()) }
    func reset()     { viewModel.emit(event: CounterEvent.Reset()) }

    deinit {
        handle?.dispose()
        viewModel.dispose()
    }
}
```

---

## Next steps

- [Core Concepts](concepts.md) — understand Event, State, and Effect in depth
- [Error Handling](error-handling.md) — register handlers and build reusable strategies
- [Testing](testing.md) — write deterministic unit tests with Turbine and Mockative
- [iOS Integration](ios-integration.md) — Swift observation patterns and lifecycle management
- [API Reference](api-reference.md) — full public API listing
