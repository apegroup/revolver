---
type: API Reference
title: API reference
description: Every public type and function in com.umain.revolver and com.umain.revolver.flow.
tags: [api, reference, kotlin]
resource: https://github.com/apegroup/revolver/tree/main/revolver/src
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T21:23:43Z }
sources:
  - id: common-src
    resource: https://github.com/apegroup/revolver/tree/main/revolver/src/commonMain/kotlin/com/umain/revolver
    title: commonMain sources
  - id: ios-src
    resource: https://github.com/apegroup/revolver/tree/main/revolver/src/iosMain/kotlin/com/umain/revolver
    title: iosMain sources
---

# API reference

Core types are in `com.umain.revolver`. Flow wrappers are in `com.umain.revolver.flow`.[^common-src]

## Marker interfaces

```kotlin
interface RevolverEvent
interface RevolverState
interface RevolverEffect
```

Implement each one with a sealed class. See [Core concepts](concepts.md).

## Handler types

```kotlin
typealias StateEmitter<STATE> = (state: STATE) -> Unit
typealias EffectEmitter<EFFECT> = (effect: EFFECT) -> Unit

interface Emitter<STATE, EFFECT> {
    val state: StateEmitter<STATE>   // sets the current state immediately
    val effect: EffectEmitter<EFFECT> // delivers a one-shot effect asynchronously
}

typealias EventHandler<EVENT, STATE, EFFECT> = suspend (event: EVENT, emit: Emitter<STATE, EFFECT>) -> Unit
typealias ErrorHandler<ERROR, STATE, EFFECT> = suspend (exception: ERROR, emit: Emitter<STATE, EFFECT>) -> Unit
```

## `RevolverViewModelInterface<EVENT, STATE, EFFECT>`

The public contract of a ViewModel. Code that uses a ViewModel can depend on this interface instead of the concrete class.

| Member | Type | Description |
|---|---|---|
| `state` | `CStateFlow<STATE>` | Current state. Always holds a value. |
| `effect` | `CSharedFlow<EFFECT>` | One-shot effects. Not replayed. |
| `emit(event)` | `(EVENT) -> Unit` | Queues an event for processing. |

## `RevolverViewModel<EVENT, STATE, EFFECT>(initialState: STATE)`

`open class`. It implements `RevolverViewModelInterface` and extends `BaseViewModel`.

| Function | Behaviour |
|---|---|
| `addEventHandler<T : EVENT>(handler: EventHandler<T, STATE, EFFECT>)` | Registers the handler for event type `T`. Throws `IllegalStateException` if `T` already has a handler. |
| `addErrorHandler<ERROR : Throwable>(handler: ErrorHandler<ERROR, STATE, EFFECT>)` | Registers a handler for exceptions of type `ERROR`. Handlers are tried in registration order, and the first match wins. Throws `IllegalStateException` if `ERROR` already has a handler. |
| `addErrorHandler<ERROR : Throwable>(errorHandler: RevolverErrorHandler<STATE, EFFECT, ERROR>)` | Same as above, for a reusable handler object. |
| `emit(event: EVENT)` | Puts the event on a 64-slot buffered channel. Events are handled one at a time, in order. Throws `IllegalStateException` if the channel can't accept the event. |

`internalEventHandler` and `internalErrorHandler` are public only because the inline functions above call them. Don't call them directly.

## `RevolverErrorHandler<STATE, EFFECT, ERROR>`

```kotlin
interface RevolverErrorHandler<STATE, EFFECT, ERROR> {
    suspend fun handleError(exception: ERROR, emit: Emitter<STATE, EFFECT>)
}
```

## `RevolverDefaultErrorHandler<STATE, EFFECT>(genericErrorState: STATE)`

A `RevolverErrorHandler` for `Throwable`. It logs the exception through Napier at error level and emits `genericErrorState`.

## `BaseViewModel`

`expect open class` that provides `viewModelScope: CoroutineScope`, running on `Dispatchers.Main`.

| Platform | Lifecycle |
|---|---|
| Android | Extends AndroidX `ViewModel`. The scope is cancelled in `onCleared()`. |
| iOS | Call `dispose()` to cancel the scope.[^ios-src] |

## Flow wrappers

| Type | Wraps | Exposed as |
|---|---|---|
| `CFlow<T : Any>(flow, coroutineScope)` | `Flow<T>` | — |
| `CSharedFlow<T : Any>(flow, coroutineScope)` | `SharedFlow<T>` | `effect` |
| `CStateFlow<T : Any>(flow, coroutineScope)` | `StateFlow<T>` | `state` |

Extension builders: `Flow<T>.cFlow()`, `SharedFlow<T>.cSharedFlow()` and `StateFlow<T>.cStateFlow()`. Each takes an optional `coroutineScope`, which defaults to `GlobalScope`.

On Android the wrappers just delegate to the wrapped flow. On iOS they add:

| Member | Description |
|---|---|
| `watch(onNext: (T) -> Unit): DisposableHandle` | Collects on `Dispatchers.Main` in the wrapper's scope. |
| `interface DisposableHandle : kotlinx.coroutines.DisposableHandle` | `dispose()` cancels the observation. |
| `operator fun DisposableHandle.plus(other)` | Returns a handle that disposes both. |

See [iOS integration](ios-integration.md) for usage.

[^common-src]: commonMain sources
[^ios-src]: iosMain sources
