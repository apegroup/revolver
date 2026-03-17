---
layout: default
title: Core Concepts
nav_order: 3
description: "Event, State, Effect, ViewModel, and Emitter explained."
---

# Core Concepts

Revolver is built around three primitives and a single rule: **data flows in one direction**.

```
Client ──emit(Event)──► ViewModel ──► EventHandler
                                           │
                         ┌─────────────────┤
                         ▼                 ▼
                   StateFlow<State>  SharedFlow<Effect>
                         │                 │
                         └──────► Client ◄─┘
```

---

## Event

An `Event` is a message sent **from the client to the ViewModel**. It describes something that happened — a user action, a lifecycle callback, or a timer firing.

```kotlin
sealed class SearchEvent : RevolverEvent {
    data class QueryChanged(val query: String) : SearchEvent()
    object ClearResults : SearchEvent()
}
```

**Rules:**
- Implement `RevolverEvent`
- Model as a sealed class
- Each subtype must have exactly one registered `EventHandler` in the ViewModel
- Events carry only what the handler needs — avoid passing pre-computed data

---

## State

A `State` describes what the UI should render **right now**. It is immutable; the ViewModel never mutates a state object — it replaces it entirely.

```kotlin
sealed class SearchState : RevolverState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Results(val items: List<String>) : SearchState()
    data class Error(val message: String) : SearchState()
}
```

**Rules:**
- Implement `RevolverState`
- Model as a sealed class
- Use `object` for states with no data, `data class` for states with a payload
- **Do not use plain `class`** — it disables structural equality, which can prevent `StateFlow` from emitting when you set the same logical state twice
- States must be complete — the client should need nothing else to render the UI

---

## Effect

An `Effect` is a **one-shot notification** to the client: navigate, show a toast, trigger haptics. Unlike state, an effect is not retained — a late subscriber will not receive it.

```kotlin
sealed class SearchEffect : RevolverEffect {
    data class NavigateToDetail(val id: String) : SearchEffect()
    data class ShowError(val message: String) : SearchEffect()
}
```

**Rules:**
- Implement `RevolverEffect`
- Model as a sealed class
- Use for events that happen **once** and must not be replayed (navigation, one-time toasts)
- Do not encode current view state in effects — that is what `State` is for

---

## ViewModel

`RevolverViewModel` is the central piece. It:

1. Holds `state: CStateFlow<STATE>` — always has a value, replayed to new subscribers
2. Holds `effect: CSharedFlow<EFFECT>` — delivered once per subscriber, not replayed
3. Accepts events via `emit(event)` — queued into a buffered channel
4. Routes each event to its registered `EventHandler` in a coroutine
5. Catches all exceptions and routes them to a registered `ErrorHandler`

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
        val results = repository.search(event.query)
        emit.state(SearchState.Results(results))
    }

    private suspend fun onClear(
        event: SearchEvent.ClearResults,
        emit: Emitter<SearchState, SearchEffect>,
    ) {
        emit.state(SearchState.Idle)
    }
}
```

---

## Emitter

The `Emitter<STATE, EFFECT>` is passed into every `EventHandler` and `ErrorHandler`. It is the only way to push new values from inside a handler:

| Function | What it does |
|---|---|
| `emit.state(newState)` | Replaces the current state synchronously |
| `emit.effect(newEffect)` | Broadcasts a one-shot effect asynchronously |

A handler may call `emit.state(...)` multiple times to produce intermediate states (e.g. `Loading` → `Loaded`). Each call is observed immediately by subscribers.

---

## Handler registration order

Both `addEventHandler` and `addErrorHandler` use a `Map<KClass<*>, Handler>`, so:

- Each event/exception type can have **at most one** handler
- For error handlers, matching is done in **insertion order** — register specific subtypes before base types:

```kotlin
init {
    addErrorHandler<NetworkException>(::onNetworkError)  // checked first
    addErrorHandler<IOException>(::onIOError)            // checked second
    addErrorHandler<Exception>(::onGenericError)         // fallback
}
```

If a thrown exception matches `NetworkException`, the `IOException` and `Exception` handlers are never invoked.
