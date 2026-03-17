package com.umain.advancedandroidintegration.presentation

import com.umain.revolver.RevolverState

/**
 * Immutable snapshot of what the UI should render.
 *
 * Use `data object` for states without a payload and `data class` for states that carry data.
 * Avoid plain `class` — it disables structural equality and can suppress StateFlow emissions.
 */
sealed class MainViewState : RevolverState {

    /** Initial state and state while loading is in progress. */
    data object Loading : MainViewState()

    /**
     * The list has been loaded successfully.
     *
     * @param items the fetched item labels to display.
     * @param isRefreshing true while a background refresh is running over an existing list.
     */
    data class Loaded(
        val items: List<String>,
        val isRefreshing: Boolean = false,
    ) : MainViewState()

    /**
     * Loading failed.
     *
     * @param message a human-readable description of the failure.
     * @param retryable whether the UI should offer a retry action.
     */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : MainViewState()
}
