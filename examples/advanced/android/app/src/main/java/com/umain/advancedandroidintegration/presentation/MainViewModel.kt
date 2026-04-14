package com.umain.advancedandroidintegration.presentation

import com.umain.revolver.Emitter
import com.umain.revolver.RevolverEffect
import com.umain.revolver.RevolverViewModel
import kotlinx.coroutines.delay

/**
 * Demonstrates multiple event types, chained state transitions, effect emission,
 * and a custom reusable [ItemErrorHandler].
 *
 * Note: [RevolverEffect] is used as the EFFECT type parameter here because this ViewModel
 * defines its own [MainViewEffect] sealed class — [RevolverEffect] is the common base.
 */
class MainViewModel : RevolverViewModel<MainViewEvent, MainViewState, MainViewEffect>(
    initialState = MainViewState.Loading,
) {

    // Toggle to simulate a transient failure on the first load attempt.
    private var failNextLoad = true

    init {
        addEventHandler<MainViewEvent.LoadItems>(::onLoadItems)
        addEventHandler<MainViewEvent.RetryAfterError>(::onRetry)

        // Custom reusable handler: maps any Exception to MainViewState.Error.
        addErrorHandler(ItemErrorHandler { message -> MainViewState.Error(message) })
    }

    private suspend fun onLoadItems(
        event: MainViewEvent.LoadItems,
        emit: Emitter<MainViewState, MainViewEffect>,
    ) {
        emit.state(MainViewState.Loading)
        delay(1_500L)

        // Simulate a failure on the first attempt to demonstrate error + retry flow.
        if (failNextLoad) {
            failNextLoad = false
            throw Exception("Failed to load items. Check your connection.")
        }

        val items = fetchItems()
        emit.state(MainViewState.Loaded(items))
        emit.effect(MainViewEffect.ShowToast("${items.size} items loaded"))
    }

    private suspend fun onRetry(
        event: MainViewEvent.RetryAfterError,
        emit: Emitter<MainViewState, MainViewEffect>,
    ) {
        emit.state(MainViewState.Loading)
        delay(1_500L)

        val items = fetchItems()
        emit.state(MainViewState.Loaded(items))
        emit.effect(MainViewEffect.ShowToast("Loaded successfully!"))
    }

    /** Simulates a remote data source. Replace with a real repository in production. */
    private fun fetchItems(): List<String> = listOf(
        "Kotlin Multiplatform",
        "Jetpack Compose",
        "Coroutines & Flow",
        "Revolver State Management",
        "Clean Architecture",
        "Unidirectional Data Flow",
    )
}
