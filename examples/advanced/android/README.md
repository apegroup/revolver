# Advanced Android Integration Example

This example builds on the basic integration to demonstrate real-world Revolver patterns.

## What it shows

| Pattern | Where |
|---|---|
| Multiple event types | `MainViewEvent` — `LoadItems`, `RetryAfterError` |
| Chained state transitions | `onLoadItems`: emits `Loading` → then `Loaded` or throws |
| Error state with retry | `MainViewState.Error(message, retryable)` |
| One-shot effect | `MainViewEffect.ShowToast` collected in `LaunchedEffect` |
| Custom reusable error handler | `ItemErrorHandler<STATE, EFFECT>(toErrorState)` |
| First-load failure simulation | `failNextLoad` flag in `MainViewModel` — tap Retry to recover |

## Running the example

```bash
export GH_USERNAME=<your-github-username>
export GH_TOKEN=<your-github-token-with-read:packages>

cd examples/advanced/android
./gradlew installDebug
```

## Flow walkthrough

1. App launches → `LoadItems` event emitted → `Loading` state shown
2. After 1.5 s → simulated failure throws `Exception` → `ItemErrorHandler` catches it → `Error` state shown with **Retry** button
3. User taps **Retry** → `RetryAfterError` event → `Loading` again → `Loaded` state with item list + `ShowToast` effect
4. User taps **Refresh** → `LoadItems` again → this time succeeds immediately (flag reset)

## Key files

```
app/src/main/java/com/umain/advancedandroidintegration/
├── MainActivity.kt                     # Compose UI, state/effect collection
└── presentation/
    ├── MainViewEvent.kt                # LoadItems, RetryAfterError
    ├── MainViewState.kt                # Loading, Loaded, Error
    ├── MainViewEffect.kt               # ShowToast
    ├── ItemErrorHandler.kt             # Reusable RevolverErrorHandler with factory lambda
    └── MainViewModel.kt               # Event handlers, error handler registration
```
