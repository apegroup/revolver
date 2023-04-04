package com.umain.revolver

typealias StateEmitter<STATE> = (state: STATE) -> Unit
typealias EffectEmitter<EFFECT> = (effect: EFFECT) -> Unit

/**
 * Used internally in the event handlers in ViewModels to emit new states or effects
 */
interface Emitter<STATE, EFFECT> {
    val state: StateEmitter<STATE>
    val effect: EffectEmitter<EFFECT>
}

/**
 * The method signature for an event handler in a ViewModel.
 *
 * Example event handler:
 * ```
 * private fun onSubmitted(
 *     event: MarketPickerEvent.Submitted,
 *     emit: MarketPickerEmitter,
 * ) { ... }
 * ```
 */
typealias EventHandler<EVENT, STATE, EFFECT> = suspend (
    event: EVENT,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

typealias ErrorHandler<ERROR, STATE, EFFECT> = suspend (
    exception: ERROR,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

/**
 * Emitted by the clients to the KMM ViewModel when something has happened that needs to be handled.
 * E.g. User selected the language in the market selector
 */
interface Event

/**
 * Emitted by KMM to the clients and describes the view that should be shown by the clients.
 * E.g. A list of markets to show in the market picker
 */
interface State

/**
 * Emitted by KMM to the clients to inform them that something has happened which they need to handle.
 * E.g. KMM has completed app initialization, splash screen can be dismissed.
 */
interface Effect


