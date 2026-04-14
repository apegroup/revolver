package com.umain.revolver

/**
 * Functional interface for emitting a new [RevolverState].
 */
typealias StateEmitter<STATE> = (state: STATE) -> Unit

/**
 * Functional interface for emitting a new [RevolverEffect].
 */
typealias EffectEmitter<EFFECT> = (effect: EFFECT) -> Unit

/**
 * Used within event handlers in [RevolverViewModel] to emit new states or effects.
 *
 * It provides separate emitters for [STATE] and [EFFECT].
 */
interface Emitter<STATE, EFFECT> {
    /**
     * Emitter for [STATE] changes.
     */
    val state: StateEmitter<STATE>

    /**
     * Emitter for [EFFECT] emissions.
     */
    val effect: EffectEmitter<EFFECT>
}

/**
 * The method signature for an event handler in a [RevolverViewModel].
 *
 * An event handler is a suspend function that takes an [EVENT] and an [Emitter],
 * and uses the emitter to update the UI state or trigger side effects.
 *
 * Example event handler:
 * ```kotlin
 * private suspend fun onSubmitted(
 *     event: MyEvent.Submitted,
 *     emit: Emitter<MyState, MyEffect>,
 * ) {
 *     emit.state(MyState.Loading)
 *     // ... process event ...
 *     emit.state(MyState.Success)
 * }
 * ```
 */
typealias EventHandler<EVENT, STATE, EFFECT> = suspend (
    event: EVENT,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

/**
 * The method signature for an error handler in a [RevolverViewModel].
 *
 * Similar to [EventHandler], but triggered when an exception of type [ERROR] is thrown
 * during event processing.
 */
typealias ErrorHandler<ERROR, STATE, EFFECT> = suspend (
    exception: ERROR,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

/**
 * Base interface for all events emitted by clients to a [RevolverViewModel].
 *
 * Events typically represent user actions (e.g., button clicks) or system events
 * (e.g., screen ready).
 */
interface RevolverEvent

/**
 * Base interface for all immutable states emitted by a [RevolverViewModel].
 *
 * States represent the current data and UI configuration to be rendered by the client.
 */
interface RevolverState

/**
 * Base interface for all side effects emitted by a [RevolverViewModel].
 *
 * Effects represent one-time actions like navigation, showing a toast, or playing a sound.
 */
interface RevolverEffect


