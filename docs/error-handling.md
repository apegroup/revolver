# Error Handling

All exceptions that escape an `EventHandler` are automatically caught by `RevolverViewModel` and routed to a registered `ErrorHandler`. This keeps Kotlin exceptions from propagating to platform clients as unhandled crashes.

---

## The rule: always register at least one handler

If an exception is thrown and no handler matches it, it is silently swallowed. Register a generic fallback at minimum.

```kotlin
init {
    addErrorHandler(RevolverDefaultErrorHandler(MyState.Error("Unexpected error")))
}
```

---

## Built-in: `RevolverDefaultErrorHandler`

The simplest option. Maps **any** `Throwable` to a single fixed state, and logs at `ERROR` level via Napier.

```kotlin
class MyViewModel : RevolverViewModel<MyEvent, MyState, MyEffect>(MyState.Loading) {

    init {
        addEventHandler<MyEvent.Load>(::onLoad)
        addErrorHandler(RevolverDefaultErrorHandler(MyState.Error("Something went wrong")))
    }
}
```

Use this when you have one generic error state and do not need to distinguish between exception types.

---

## Inline lambda handlers

For per-ViewModel logic without a reusable class:

```kotlin
init {
    addErrorHandler<NetworkException> { exception, emit ->
        emit.state(MyState.Offline)
        emit.effect(MyEffect.ShowToast("No connection"))
    }
    addErrorHandler<Exception> { exception, emit ->
        emit.state(MyState.Error(exception.message ?: "Unknown error"))
    }
}
```

Or with named suspend functions (easier to test in isolation):

```kotlin
init {
    addErrorHandler<NetworkException>(::onNetworkError)
    addErrorHandler<Exception>(::onGenericError)
}

private suspend fun onNetworkError(
    exception: NetworkException,
    emit: Emitter<MyState, MyEffect>,
) {
    emit.state(MyState.Offline)
    emit.effect(MyEffect.ShowToast("No connection. Check your network and retry."))
}

private suspend fun onGenericError(
    exception: Exception,
    emit: Emitter<MyState, MyEffect>,
) {
    emit.state(MyState.Error(exception.message ?: "An unexpected error occurred"))
}
```

---

## Registration order matters

Error handlers are matched in the order they were registered using `isInstance` checks. Register more specific subtypes **before** broader ones:

```kotlin
init {
    // ✅ Correct — specific before generic
    addErrorHandler<NetworkException>(::onNetworkError)   // matched first
    addErrorHandler<IOException>(::onIOError)             // matched second
    addErrorHandler<Exception>(::onGenericError)          // fallback

    // ❌ Wrong — Exception catches everything, NetworkException never fires
    // addErrorHandler<Exception>(::onGenericError)
    // addErrorHandler<NetworkException>(::onNetworkError)
}
```

---

## Reusable error handlers

When the same error logic applies across multiple ViewModels, implement `RevolverErrorHandler<STATE, EFFECT, ERROR>`:

```kotlin
class NetworkErrorHandler<STATE : RevolverState, EFFECT : RevolverEffect>(
    private val offlineState: STATE,
    private val offlineEffect: EFFECT? = null,
) : RevolverErrorHandler<STATE, EFFECT, NetworkException> {

    override suspend fun handleError(
        exception: NetworkException,
        emit: Emitter<STATE, EFFECT>,
    ) {
        emit.state(offlineState)
        offlineEffect?.let { emit.effect(it) }
    }
}
```

Register in any ViewModel:

```kotlin
class HomeViewModel : RevolverViewModel<HomeEvent, HomeState, HomeEffect>(HomeState.Loading) {
    init {
        addErrorHandler(NetworkErrorHandler(HomeState.Offline, HomeEffect.ShowOfflineBanner))
        addErrorHandler(RevolverDefaultErrorHandler(HomeState.Error("Unexpected error")))
    }
}

class ProfileViewModel : RevolverViewModel<ProfileEvent, ProfileState, ProfileEffect>(ProfileState.Loading) {
    init {
        addErrorHandler(NetworkErrorHandler(ProfileState.Offline))
        addErrorHandler(RevolverDefaultErrorHandler(ProfileState.Error("Unexpected error")))
    }
}
```

---

## Testing error handlers

Error handlers are tested exactly like event handlers — emit an event whose handler throws, then assert the emitted state.

```kotlin
@Test
fun networkErrorEmitsOfflineState() = runTest {
    given(repository).coroutine { load() }.thenThrow(NetworkException())

    val viewModel = MyViewModel(repository)

    viewModel.state.test {
        viewModel.emit(MyEvent.Load)

        assertIs<MyState.Loading>(awaitItem())
        assertIs<MyState.Offline>(awaitItem())
    }
}
```

See the full [Testing guide](testing.md) for setup and patterns.
