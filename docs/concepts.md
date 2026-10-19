---
type: Concept
title: Core concepts
description: Events, states, effects, the ViewModel and the emitter, plus how events and errors are processed at runtime.
tags: [architecture, event, state, effect, viewmodel]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T22:05:00Z }
sources:
  - id: viewmodel-src
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonMain/kotlin/com/umain/revolver/RevolverViewModel.kt
    title: RevolverViewModel.kt
  - id: types-src
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonMain/kotlin/com/umain/revolver/Types.kt
    title: Types.kt
---

# Core concepts

Revolver follows one rule: **data flows in one direction**. Clients send events in. The ViewModel sends states and effects out.

```
Client ──emit(Event)──► RevolverViewModel ──► EventHandler
                                                   │
                             ┌─────────────────────┤
                             ▼                     ▼
                       StateFlow<State>    SharedFlow<Effect>
                             │                     │
                             └──────► Client ◄─────┘
```

| Type | Direction | Purpose |
|---|---|---|
| `RevolverEvent` | Client → ViewModel | A user action or lifecycle trigger |
| `RevolverState` | ViewModel → Client | An immutable snapshot of what to display |
| `RevolverEffect` | ViewModel → Client | A one-shot side effect, such as navigation or a toast |

## Event

An event says that something happened: a tap, a screen appearing, a timer firing. Model events as a sealed class that implements `RevolverEvent`, and register exactly one handler for each subtype.

```kotlin
sealed class SearchEvent : RevolverEvent {
    data class QueryChanged(val query: String) : SearchEvent()
    object ClearResults : SearchEvent()
}
```

## State

A state is a complete description of what the UI should render right now. The ViewModel never mutates a state. It replaces the whole value.

```kotlin
sealed class SearchState : RevolverState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Results(val items: List<String>) : SearchState()
    data class Error(val message: String) : SearchState()
}
```

Use `object` for states with no data and `data class` for states with a payload. `state` is a `StateFlow`, which skips any value equal to the current one. With `data class`, setting the same logical state twice is a no-op. A plain `class` compares by identity, so every call to `emit.state` reaches the UI even when nothing changed.

## Effect

An effect is a one-shot instruction to the client, such as navigating, showing a toast or triggering haptics. Effects are not stored. A collector that subscribes late never sees earlier effects. Don't put view state in an effect; that belongs in `State`.

```kotlin
sealed class SearchEffect : RevolverEffect {
    data class NavigateToDetail(val id: String) : SearchEffect()
}
```

## ViewModel

Extend `RevolverViewModel<EVENT, STATE, EFFECT>`, pass an `initialState`, and register handlers in `init`:

```kotlin
class SearchViewModel(
    private val repository: SearchRepository,
) : RevolverViewModel<SearchEvent, SearchState, SearchEffect>(
    initialState = SearchState.Idle,
) {
    init {
        addEventHandler<SearchEvent.QueryChanged>(::onQueryChanged)
        addEventHandler<SearchEvent.ClearResults>(::onClear)
        addErrorHandler(RevolverDefaultErrorHandler(SearchState.Error("Something went wrong")))
    }

    private suspend fun onQueryChanged(
        event: SearchEvent.QueryChanged,
        emit: Emitter<SearchState, SearchEffect>,
    ) {
        emit.state(SearchState.Loading)
        emit.state(SearchState.Results(repository.search(event.query)))
    }

    private suspend fun onClear(
        event: SearchEvent.ClearResults,
        emit: Emitter<SearchState, SearchEffect>,
    ) {
        emit.state(SearchState.Idle)
    }
}
```

It exposes `state: CStateFlow<STATE>`, `effect: CSharedFlow<EFFECT>` and `emit(event)`. It also exposes `stateFlow` and `effectFlow`: the same streams typed as plain `StateFlow` and `SharedFlow`, for Swift export. On Android it extends AndroidX `ViewModel`. On iOS you call `dispose()` yourself (see [iOS integration](ios-integration.md)).

## Emitter

Every event handler and error handler receives an `Emitter<STATE, EFFECT>`. It is the only way to publish from inside a handler.

| Call | Effect |
|---|---|
| `emit.state(newState)` | Sets the current state immediately. |
| `emit.effect(newEffect)` | Launches a coroutine that delivers the effect to current collectors. |

A handler can call `emit.state` several times to show intermediate states, for example `Loading` then `Results`.

## Runtime behaviour

These rules come straight from the implementation.[^viewmodel-src]

- **Events are processed one at a time, in order.** `emit` puts the event on a buffered channel, and a single collector runs the matching handlers one after another. A handler that suspends, for example on a network call, holds back every event behind it. To run work in parallel, launch it in `viewModelScope` from the handler.
- **`emit` throws if the buffer is full.** The channel holds 64 pending events. If it can't accept another, `emit` throws `IllegalStateException`.
- **An event without a handler is an error.** The ViewModel throws `IllegalStateException`, which goes through the error handlers like any other exception.
- **Error handlers are matched in registration order.** The first handler whose type matches the exception (`isInstance`) wins. Register specific types before general ones.
- **Unmatched exceptions are dropped.** The ViewModel logs them through Napier at warning level and continues. Always register a catch-all, such as `RevolverDefaultErrorHandler`.
- **Effects with no collector are lost.** `effect` is a `SharedFlow` with no replay, so if nothing is collecting when an effect is emitted, it is discarded.
- **Each type gets one handler.** Registering a second event handler or error handler for the same type throws `IllegalStateException`.

The handler signatures are defined as type aliases.[^types-src] See the [API reference](api-reference.md) for the complete list.

[^viewmodel-src]: RevolverViewModel.kt
[^types-src]: Types.kt
