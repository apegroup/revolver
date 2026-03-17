package com.umain.revolver

/** Callback type used by [Emitter] to deliver a new [STATE] value. */
typealias StateEmitter<STATE> = (state: STATE) -> Unit

/** Callback type used by [Emitter] to deliver a one-shot [EFFECT] value. */
typealias EffectEmitter<EFFECT> = (effect: EFFECT) -> Unit

/**
 * Passed to every [EventHandler] and [ErrorHandler] so they can push new states and effects
 * without holding a direct reference to the ViewModel internals.
 *
 * @param STATE the [RevolverState] type for this ViewModel.
 * @param EFFECT the [RevolverEffect] type for this ViewModel.
 */
interface Emitter<STATE, EFFECT> {
    /** Emits a new [STATE], immediately replacing the current value in the StateFlow. */
    val state: StateEmitter<STATE>

    /** Emits a one-shot [EFFECT] to the SharedFlow. Collected once per active subscriber. */
    val effect: EffectEmitter<EFFECT>
}

/**
 * Suspend function type for handling a specific [EVENT] subtype inside a [RevolverViewModel].
 * Registered via [RevolverViewModel.addEventHandler].
 *
 * Each event type may have at most one handler. The handler receives the typed event and an
 * [Emitter] for producing state and effect updates.
 *
 * Example:
 * ```kotlin
 * private suspend fun onRefresh(
 *     event: ExampleEvent.Refresh,
 *     emit: Emitter<ExampleState, ExampleEffect>,
 * ) {
 *     emit.state(ExampleState.Loading)
 *     emit.state(ExampleState.Loaded(fetchData()))
 * }
 * ```
 */
typealias EventHandler<EVENT, STATE, EFFECT> = suspend (
    event: EVENT,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

/**
 * Suspend function type for handling a specific [ERROR] subtype inside a [RevolverViewModel].
 * Registered via [RevolverViewModel.addErrorHandler].
 *
 * Handlers are matched in registration order — register more specific exception types before
 * generic ones.
 */
typealias ErrorHandler<ERROR, STATE, EFFECT> = suspend (
    exception: ERROR,
    emit: Emitter<STATE, EFFECT>,
) -> Unit

/**
 * Marker interface for all events sent from clients to a [RevolverViewModel].
 *
 * Implement with a sealed class to enumerate every action the ViewModel can handle:
 * ```kotlin
 * sealed class ExampleEvent : RevolverEvent {
 *     object Refresh : ExampleEvent()
 *     data class Search(val query: String) : ExampleEvent()
 * }
 * ```
 */
interface RevolverEvent

/**
 * Marker interface for all immutable view states emitted by a [RevolverViewModel].
 *
 * Implement with a sealed class. Prefer `object` for states without data and `data class`
 * for states that carry payloads — plain `class` can suppress emissions when the value does
 * not structurally change.
 *
 * ```kotlin
 * sealed class ExampleState : RevolverState {
 *     object Loading : ExampleState()
 *     data class Loaded(val items: List<String>) : ExampleState()
 *     data class Error(val message: String) : ExampleState()
 * }
 * ```
 */
interface RevolverState

/**
 * Marker interface for one-shot side effects emitted by a [RevolverViewModel].
 *
 * Effects are delivered via a SharedFlow and consumed once per subscriber. Use them for
 * actions that must not be replayed on resubscription, such as navigation or toasts.
 *
 * ```kotlin
 * sealed class ExampleEffect : RevolverEffect {
 *     data class ShowToast(val message: String) : ExampleEffect()
 *     data class NavigateTo(val route: String) : ExampleEffect()
 * }
 * ```
 */
interface RevolverEffect


