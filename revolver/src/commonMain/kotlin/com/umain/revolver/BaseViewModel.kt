package com.umain.revolver

import kotlinx.coroutines.CoroutineScope

/**
 * Platform-specific base for [RevolverViewModel]. Provides a [viewModelScope] tied to the
 * platform's lifecycle:
 * - **Android**: backed by AndroidX `ViewModel`; scope is cancelled in `onCleared()`.
 * - **iOS**: scope must be cancelled manually by calling `dispose()`.
 */
expect open class BaseViewModel() {
    /**
     * The coroutine scope used for all ViewModel work. Cancelled automatically on Android
     * when the ViewModel is cleared; call [dispose][com.umain.revolver.BaseViewModel.dispose]
     * manually on iOS.
     */
    val viewModelScope: CoroutineScope
}
