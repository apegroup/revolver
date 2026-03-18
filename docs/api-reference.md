# API Reference

All public symbols are in the `com.umain.revolver` package. Flow wrappers are in `com.umain.revolver.flow`.

---

## Marker interfaces

### `RevolverEvent`

```kotlin
interface RevolverEvent
```

Marker interface for all events sent from clients to a `RevolverViewModel`. Implement with a sealed class to enumerate every action the ViewModel can handle.

---

### `RevolverState`

```kotlin
interface RevolverState
```

Marker interface for all immutable view states emitted by a `RevolverViewModel`. Implement with a sealed class. Prefer `object` for states without data and `data class` for states that carry payloads.

---

### `RevolverEffect`

```kotlin
interface RevolverEffect
```

Marker interface for one-shot side effects emitted by a `RevolverViewModel`. Effects are delivered via a `SharedFlow` and not replayed to late collectors.

---

## Emitter

```kotlin
interface Emitter<STATE, EFFECT>
```

Passed to every `EventHandler` and `ErrorHandler`. The only way to push values from inside a handler.

| Member | Type | Description |
|---|---|---|
| `state` | `StateEmitter<STATE>` | Replaces the current state immediately. |
| `effect` | `EffectEmitter<EFFECT>` | Broadcasts a one-shot effect asynchronously. |

```kotlin
typealias StateEmitter<STATE>   = (state: STATE) -> Unit
typealias EffectEmitter<EFFECT> = (effect: EFFECT) -> Unit
```

---

## Function types

### `EventHandler`

```kotlin
typealias EventHandler<EVENT, STATE, EFFECT> = suspend (
    event: EVENT,
    emit: Emitter<STATE, EFFECT>,
) -> Unit
```

Suspend function that processes a single `EVENT` subtype. Registered via `RevolverViewModel.addEventHandler`. Each event type may have at most one handler.

---

### `ErrorHandler`

```kotlin
typealias ErrorHandler<ERROR, STATE, EFFECT> = suspend (
    exception: ERROR,
    emit: Emitter<STATE, EFFECT>,
) -> Unit
```

Suspend function that handles a specific exception type. Registered via `RevolverViewModel.addErrorHandler`. Matched in registration order.

---

## RevolverViewModelInterface

```kotlin
interface RevolverViewModelInterface<EVENT, STATE, EFFECT>
```

Public contract for a Revolver ViewModel. Prefer depending on this interface in consumers rather than the concrete `RevolverViewModel` class.

| Member | Type | Description |
|---|---|---|
| `state` | `CStateFlow<STATE>` | Current view state. Always holds the last emitted value. |
| `effect` | `CSharedFlow<EFFECT>` | One-shot side effects. Not replayed to late subscribers. |
| `emit(event)` | `fun (EVENT)` | Dispatches an event for asynchronous processing. |

---

## RevolverViewModel

```kotlin
open class RevolverViewModel<EVENT, STATE, EFFECT>(
    initialState: STATE,
) : RevolverViewModelInterface<EVENT, STATE, EFFECT>, BaseViewModel()
```

Base class for all Revolver ViewModels. Handles event routing, error catching, and lifecycle management.

**Constructor parameters:**

| Parameter | Description |
|---|---|
| `initialState` | The state exposed by `state` before any event is processed. |

**Methods:**

#### `addEventHandler`

```kotlin
inline fun <reified T : EVENT> addEventHandler(
    noinline handler: EventHandler<T, STATE, EFFECT>
)
```

Registers a suspend lambda as the handler for event type `T`. Call in `init`. Each type may have at most one handler.

**Throws** `IllegalStateException` if a handler for `T` is already registered.

---

#### `addErrorHandler` (lambda)

```kotlin
inline fun <reified ERROR : Throwable> addErrorHandler(
    noinline handler: ErrorHandler<ERROR, STATE, EFFECT>
)
```

Registers a suspend lambda as the handler for exception type `ERROR`. Handlers are matched in registration order.

**Throws** `IllegalStateException` if a handler for `ERROR` is already registered.

---

#### `addErrorHandler` (interface)

```kotlin
inline fun <reified ERROR : Throwable> addErrorHandler(
    errorHandler: RevolverErrorHandler<STATE, EFFECT, ERROR>
)
```

Registers a `RevolverErrorHandler` instance. Delegates to the lambda overload using `errorHandler::handleError`.

---

#### `emit`

```kotlin
override fun emit(event: EVENT)
```

Sends an event to the ViewModel for asynchronous processing by its registered `EventHandler`.

**Throws** `IllegalStateException` if the internal event channel cannot accept the event.

---

## RevolverErrorHandler

```kotlin
interface RevolverErrorHandler<STATE, EFFECT, ERROR>
```

Interface for reusable error handlers shared across multiple ViewModels.

#### `handleError`

```kotlin
suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
```

Called when an exception of type `ERROR` is caught by the ViewModel.

---

## RevolverDefaultErrorHandler

```kotlin
class RevolverDefaultErrorHandler<STATE, EFFECT>(
    genericErrorState: STATE,
) : RevolverErrorHandler<STATE, EFFECT, Throwable>
```

Built-in catch-all handler that maps any `Throwable` directly to a fixed error state. Logs the exception via Napier at `ERROR` level.

| Parameter | Description |
|---|---|
| `genericErrorState` | The state emitted whenever any exception is caught. |

```kotlin
// Usage
addErrorHandler(RevolverDefaultErrorHandler(MyState.Error("Something went wrong")))
```

---

## BaseViewModel

```kotlin
expect open class BaseViewModel()
```

Platform-specific base. Provides a `viewModelScope` tied to the platform lifecycle.

| Platform | Lifecycle behaviour |
|---|---|
| Android | Backed by AndroidX `ViewModel`. Scope cancelled in `onCleared()`. |
| iOS | Scope cancelled by calling `dispose()` manually. |

| Member | Type | Description |
|---|---|---|
| `viewModelScope` | `CoroutineScope` | Scope for all ViewModel coroutines. |
| `dispose()` *(iOS only)* | `fun` | Cancels `viewModelScope`. Must be called from Swift `deinit`. |

---

## Flow wrappers (`com.umain.revolver.flow`)

These wrappers exist to provide a Swift-friendly API on iOS without exposing the full coroutines API to Objective-C/Swift.

### `CFlow<T>`

```kotlin
expect class CFlow<out T : Any>(
    flow: Flow<T>,
    coroutineScope: CoroutineScope,
) : Flow<T>
```

Platform-aware `Flow` wrapper. On iOS adds `watch(onNext)`.

#### `watch` *(iOS only)*

```kotlin
fun watch(onNext: (T) -> Unit): DisposableHandle
```

Subscribes to the flow from Swift on the main dispatcher. Returns a `DisposableHandle` to cancel the subscription.

#### Extension: `cFlow`

```kotlin
fun <T : Any> Flow<T>.cFlow(coroutineScope: CoroutineScope = GlobalScope): CFlow<T>
```

---

### `CSharedFlow<T>`

```kotlin
expect open class CSharedFlow<out T : Any>(
    flow: SharedFlow<T>,
    coroutineScope: CoroutineScope,
) : SharedFlow<T>
```

Platform-aware `SharedFlow` wrapper. Exposed as `RevolverViewModelInterface.effect`. On iOS inherits `watch`.

#### Extension: `cSharedFlow`

```kotlin
fun <T : Any> SharedFlow<T>.cSharedFlow(coroutineScope: CoroutineScope = GlobalScope): CSharedFlow<T>
```

---

### `CStateFlow<T>`

```kotlin
expect open class CStateFlow<out T : Any>(
    flow: StateFlow<T>,
    coroutineScope: CoroutineScope,
) : StateFlow<T>
```

Platform-aware `StateFlow` wrapper. Always holds the latest `value`. Exposed as `RevolverViewModelInterface.state`. On iOS inherits `watch`.

#### Extension: `cStateFlow`

```kotlin
fun <T : Any> StateFlow<T>.cStateFlow(coroutineScope: CoroutineScope = GlobalScope): CStateFlow<T>
```

---

### `DisposableHandle` *(iOS only)*

```kotlin
interface DisposableHandle : kotlinx.coroutines.DisposableHandle
```

A cancellation handle for a running flow observation. Obtained from `CFlow.watch`. Always cancel in Swift `deinit`.

#### `dispose()`

```kotlin
fun dispose()
```

Cancels the underlying coroutine job.

#### `operator fun DisposableHandle.plus(other: DisposableHandle): DisposableHandle`

Combines two handles into one. Calling `dispose()` on the result disposes both.

```swift
let handle = stateHandle + effectHandle
// later:
handle.dispose()
```
