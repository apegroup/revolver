package com.umain.revolver.flow

/**
 * A handle to a running Flow observation that can be cancelled from Swift.
 *
 * This is a thin wrapper around `kotlinx.coroutines.DisposableHandle` that avoids
 * exporting the entire coroutines API to the Swift/Objective-C interface.
 *
 * Obtained from [CFlow.watch]. Always call [dispose] when the observer is no longer needed.
 */
interface DisposableHandle : kotlinx.coroutines.DisposableHandle

/**
 * Factory function that creates a [DisposableHandle] from a cleanup [block].
 *
 * @param block the cancellation logic to run when [DisposableHandle.dispose] is called.
 */
fun DisposableHandle(block: () -> Unit): DisposableHandle {
    return object : DisposableHandle {
        override fun dispose() {
            block()
        }
    }
}

/**
 * Combines two [DisposableHandle] instances into one. Calling [DisposableHandle.dispose] on
 * the result disposes both handles.
 *
 * Useful when observing multiple flows and wanting a single handle to cancel all of them:
 * ```swift
 * let handle = stateHandle + effectHandle
 * // later:
 * handle.dispose()
 * ```
 */
operator fun DisposableHandle.plus(other: DisposableHandle): DisposableHandle {
    return DisposableHandle {
        this.dispose()
        other.dispose()
    }
}
