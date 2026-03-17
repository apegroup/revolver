package com.umain.revolver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual open class BaseViewModel actual constructor() {
    actual val viewModelScope: CoroutineScope = createViewModelScope()

    /**
     * Cancels the [viewModelScope] and all coroutines running within it.
     *
     * **Must be called from Swift** when the owning object is deallocated (e.g. in `deinit`),
     * because iOS has no automatic lifecycle hook equivalent to Android's `onCleared()`.
     *
     * ```swift
     * deinit {
     *     viewModel.dispose()
     * }
     * ```
     */
    fun dispose() = viewModelScope.cancel()
}
