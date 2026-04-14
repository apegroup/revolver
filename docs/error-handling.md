# Error Handling in Revolver

Revolver provides a robust mechanism to handle exceptions that occur during event processing. The core philosophy is to catch all errors within the ViewModel and map them to meaningful UI states or side effects, ensuring the client never receives a raw platform exception.

## The Error Handling Flow

1. An exception is thrown inside an `EventHandler`.
2. `RevolverViewModel` catches the `Throwable`.
3. The ViewModel searches for a registered `ErrorHandler` that matches the exception type (or its supertypes).
4. If found, the handler is executed, allowing it to emit a new `State` (e.g., `ErrorState`) or an `Effect` (e.g., `ShowToast`).

## Registering Handlers

### 1. Functional Error Handlers
You can register a handler for a specific exception type directly in the `init` block:

```kotlin
class MyViewModel : RevolverViewModel<MyEvent, MyState, MyEffect>(MyState.Idle) {
    init {
        addErrorHandler<NetworkException> { exception, emit ->
            emit.state(MyState.Error("No internet connection"))
        }
    }
}
```

### 2. Reusable Error Handlers
For logic shared across multiple ViewModels, implement the `RevolverErrorHandler` interface:

```kotlin
class GlobalErrorHandler<STATE, EFFECT> : RevolverErrorHandler<STATE, EFFECT, Throwable> {
    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        // Log to crashlytics
        Logger.log(exception)
        // Trigger a generic error effect
        // emit.effect(...) 
    }
}

// In your ViewModel:
init {
    addErrorHandler(GlobalErrorHandler())
}
```

### 3. Default Error Handler
Revolver provides `RevolverDefaultErrorHandler` for a quick way to map all exceptions to a single state:

```kotlin
init {
    addErrorHandler(RevolverDefaultErrorHandler(MyState.GenericError))
}
```

## Important Considerations

- **Order Matters:** Handlers are evaluated in the order they are registered. Register more specific exception types before more general ones (e.g., `IllegalStateException` before `Exception`).
- **One Handler per Type:** Each exception type can only have one registered handler.
- **Uncaught Exceptions:** If no handler matches a thrown exception, the exception will bubble up (and potentially crash the app if not handled at the platform level), so it's best practice to always include a catch-all for `Exception` or `Throwable`.
