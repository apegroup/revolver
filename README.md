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

> Prefer `object` for states with no data and `data class` for states that carry data. Avoid plain `class` for states — it can prevent state updates from being emitted when the value does not change.

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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExampleEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    viewModel.emit(ExampleEvent.Refresh)

    when (val s = state) {
        is ExampleState.Loading -> CircularProgressIndicator()
        is ExampleState.Loaded  -> Text(s.result)
        is ExampleState.Error   -> Text(s.message)
    }
}
```

<br/>

## Error Handling

All exceptions that escape an `EventHandler` are caught by the ViewModel and routed to a registered `ErrorHandler`. You should always register at least one.

**Registration order matters** — handlers are matched in the order they were registered. Register more specific exception types before generic ones.

```kotlin
init {
    addErrorHandler<NetworkException>(::onNetworkError)   // matched first
    addErrorHandler<Exception>(::onGenericError)          // fallback
}

private suspend fun onNetworkError(
    exception: NetworkException,
    emit: Emitter<ExampleState, ExampleEffect>,
) {
    emit.state(ExampleState.Error("No connection"))
}

private suspend fun onGenericError(
    exception: Exception,
    emit: Emitter<ExampleState, ExampleEffect>,
) {
    emit.state(ExampleState.Error("Unexpected error"))
}
```

### Reusable error handlers

Implement `RevolverErrorHandler` to share error logic across multiple ViewModels:

```kotlin
class NetworkErrorHandler<STATE : RevolverState, EFFECT : RevolverEffect>(
    private val offlineState: STATE,
) : RevolverErrorHandler<STATE, EFFECT, NetworkException> {

    override suspend fun handleError(exception: NetworkException, emit: Emitter<STATE, EFFECT>) {
        emit.state(offlineState)
    }
}

// In any ViewModel:
init {
    addErrorHandler(NetworkErrorHandler(ExampleState.Error("No connection")))
}
```

For a zero-configuration fallback that maps **any** exception to a single error state, use the built-in `RevolverDefaultErrorHandler`:

```kotlin
init {
    addErrorHandler(RevolverDefaultErrorHandler(ExampleState.Error("Something went wrong")))
}
```

<br/>

## iOS Integration

The library exposes `CStateFlow`, `CSharedFlow`, and `CFlow` wrappers that are callable from Swift without importing the full coroutines API.

Use `watch(onNext:)` to observe a flow from Swift. It returns a `DisposableHandle` that **must** be cancelled when the observer is deallocated.

```swift
class ExampleObservableViewModel: ObservableObject {
    @Published var state: ExampleState = ExampleState.Loading()

    private let viewModel = ExampleViewModel()
    private var stateHandle: DisposableHandle?
    private var effectHandle: DisposableHandle?

    init() {
        stateHandle = viewModel.state.watch { [weak self] state in
            self?.state = state
        }
        effectHandle = viewModel.effect.watch { [weak self] effect in
            // handle effect
        }
        viewModel.emit(event: ExampleEvent.Refresh())
    }

    deinit {
        stateHandle?.dispose()
        effectHandle?.dispose()
    }
}
```

You can also combine multiple `DisposableHandle` instances with `+`:

```swift
let handle = stateHandle + effectHandle
// later:
handle.dispose()
```

Call `viewModel.dispose()` to cancel the underlying coroutine scope on iOS (Android handles this automatically via the `ViewModel` lifecycle).

<br/>

## Testing

Because all logic lives in `commonMain` and states are immutable, ViewModels can be tested in pure Kotlin without any client dependency.

**Recommended libraries** (already included in `commonTest`):
- [Turbine](https://github.com/cashapp/turbine) — Flow assertion DSL
- [Mockative](https://github.com/mockative/mockative) — KMP-compatible mocking
- `kotlin.test` + `kotlinx.coroutines.test`

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
internal class ExampleViewModelTests {

    @Mock
    private val repository = mock(classOf<ExampleRepository>())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun dispose() {
        Dispatchers.resetMain()
    }

    @Test
    fun onRefreshEmitsLoadingThenLoaded() = runTest {
        given(repository).coroutine { fetchData() }.thenReturn("testData")

        val viewModel = ExampleViewModel(repository, initialState = ExampleState.Loading)

        viewModel.state.test {
            viewModel.emit(ExampleEvent.Refresh)

            assertIs<ExampleState.Loading>(awaitItem())
            val loaded = assertIs<ExampleState.Loaded>(awaitItem())
            assertEquals("testData", loaded.result)
        }
    }

    @Test
    fun onRefreshEmitsShowToastEffect() = runTest {
        given(repository).coroutine { fetchData() }.thenReturn("testData")

        val viewModel = ExampleViewModel(repository, initialState = ExampleState.Loading)

        viewModel.effect.test {
            viewModel.emit(ExampleEvent.Refresh)
            val effect = assertIs<ExampleEffect.ShowToast>(awaitItem())
            assertEquals("Loaded!", effect.message)
        }
    }
}
```

<br/>

## Contribution

Bug reports, feature requests, and pull requests are welcome. This library is in active development — production use is at your own discretion.
