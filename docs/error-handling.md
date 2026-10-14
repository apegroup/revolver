---
type: Guide
title: Error handling
description: Catch exceptions from event handlers and map them to states and effects.
tags: [errors, exceptions, error-handler]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T21:23:43Z }
sources:
  - id: viewmodel-src
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonMain/kotlin/com/umain/revolver/RevolverViewModel.kt
    title: RevolverViewModel.kt
  - id: default-handler-src
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonMain/kotlin/com/umain/revolver/RevolverDefaultErrorHandler.kt
    title: RevolverDefaultErrorHandler.kt
---

# Error handling

`RevolverViewModel` catches every exception an event handler throws and passes it to the first registered error handler whose type matches. If no handler matches, the exception is logged and dropped,[^viewmodel-src] so always register a catch-all.

## Built-in catch-all

`RevolverDefaultErrorHandler` maps any `Throwable` to one fixed state and logs it through Napier at error level.[^default-handler-src]

```kotlin
init {
    addEventHandler<MyEvent.Load>(::onLoad)
    addErrorHandler(RevolverDefaultErrorHandler(MyState.Error("Something went wrong")))
}
```

## Per-type handlers

Pass a lambda or a function reference with the exception type as the type argument:

```kotlin
init {
    addErrorHandler<NetworkException>(::onNetworkError)
    addErrorHandler<Exception> { exception, emit ->
        emit.state(MyState.Error(exception.message ?: "Unknown error"))
    }
}

private suspend fun onNetworkError(
    exception: NetworkException,
    emit: Emitter<MyState, MyEffect>,
) {
    emit.state(MyState.Offline)
    emit.effect(MyEffect.ShowToast("No connection"))
}
```

## Registration order

Handlers are tried in the order they were registered, and the first match wins. Put specific types before general ones:

```kotlin
init {
    addErrorHandler<NetworkException>(::onNetworkError) // tried first
    addErrorHandler<IOException>(::onIOError)
    addErrorHandler<Exception>(::onGenericError)        // fallback
}
```

If `Exception` were registered first, `onNetworkError` would never run. Each exception type can have only one handler; registering a second one throws `IllegalStateException`.

## Reusable handlers

To share error logic across ViewModels, implement `RevolverErrorHandler<STATE, EFFECT, ERROR>`:

```kotlin
class NetworkErrorHandler<STATE : RevolverState, EFFECT : RevolverEffect>(
    private val offlineState: STATE,
) : RevolverErrorHandler<STATE, EFFECT, NetworkException> {

    override suspend fun handleError(exception: NetworkException, emit: Emitter<STATE, EFFECT>) {
        emit.state(offlineState)
    }
}

// In any ViewModel
init {
    addErrorHandler(NetworkErrorHandler(HomeState.Offline))
    addErrorHandler(RevolverDefaultErrorHandler(HomeState.Error("Unexpected error")))
}
```

To test error paths, see [Testing](testing.md#error-paths).

[^viewmodel-src]: RevolverViewModel.kt
[^default-handler-src]: RevolverDefaultErrorHandler.kt
