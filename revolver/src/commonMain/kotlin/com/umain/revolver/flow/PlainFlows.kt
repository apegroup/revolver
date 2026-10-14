package com.umain.revolver.flow

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes a flow only through its [StateFlow] interface. Swift export bridges this interface to
 * an `AsyncSequence` but cannot bridge the [CStateFlow] wrapper class, so the wrapper must not be
 * returned where a plain [StateFlow] is expected.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class PlainStateFlow<out T>(flow: StateFlow<T>) : StateFlow<T> by flow

/** [SharedFlow] counterpart of [PlainStateFlow]. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class PlainSharedFlow<out T>(flow: SharedFlow<T>) : SharedFlow<T> by flow
