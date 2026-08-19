package com.umain.revolver

import com.umain.revolver.flow.cSharedFlow
import com.umain.revolver.flow.cStateFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Base class for all Revolver ViewModels. Extend this class, parameterise it with your sealed
 * [RevolverEvent], [RevolverState], and [RevolverEffect] types, then register handlers in `init`.
 *
 * ```kotlin
 * class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(
 *     initialState = ExampleState.Loading,
 * ) {
 *     init {
 *         addEventHandler<ExampleEvent.Refresh>(::onRefresh)
 *         addErrorHandler(RevolverDefaultErrorHandler(ExampleState.Error("Oops")))
 *     }
 *
 *     private suspend fun onRefresh(
 *         event: ExampleEvent.Refresh,
 *         emit: Emitter<ExampleState, ExampleEffect>,
 *     ) {
 *         emit.state(ExampleState.Loading)
 *         emit.state(ExampleState.Loaded(fetchData()))
 *     }
 * }
 * ```
 *
 * @param EVENT sealed class implementing [RevolverEvent].
 * @param STATE sealed class implementing [RevolverState].
 * @param EFFECT sealed class implementing [RevolverEffect].
 * @param initialState the state exposed by [state] before any event is processed.
 */
open class RevolverViewModel<EVENT : RevolverEvent, STATE : RevolverState, EFFECT : RevolverEffect>(
    initialState: STATE,
) : RevolverViewModelInterface<EVENT, STATE, EFFECT>, BaseViewModel() {

    private val eventHandlers = mutableMapOf<KClass<*>, EventHandler<EVENT, STATE, EFFECT>>()
    private val errorHandlers = mutableMapOf<KClass<*>, ErrorHandler<Throwable, STATE, EFFECT>>()
    @PublishedApi internal val cancellableHandlerTypes = mutableSetOf<KClass<*>>()
    private val activeJobs = mutableMapOf<KClass<*>, Job>()

    private val events = Channel<EVENT>(Channel.BUFFERED).also {
        it.receiveAsFlow()
            .onEach(::mapEvent)
            .launchIn(viewModelScope)
    }

    private val _state = MutableStateFlow(initialState)
    private val _effect = MutableSharedFlow<EFFECT>()

    /**
     * The current view state. Always holds the last emitted [STATE] value.
     * Clients should collect this flow to drive their UI.
     */
    override val state = _state.cStateFlow(viewModelScope)

    /**
     * One-shot side effects such as navigation or toasts. Each effect is delivered once per
     * active subscriber and is not replayed to late collectors.
     */
    override val effect = _effect.cSharedFlow(viewModelScope)

    private val emitter: Emitter<STATE, EFFECT> = object : Emitter<STATE, EFFECT> {
        override val state: StateEmitter<STATE> = { state: STATE ->
            Napier.d("RevolverViewModel ${this@RevolverViewModel::class.simpleName} emitting state ${state::class.simpleName}")
            _state.value = state
        }
        override val effect: EffectEmitter<EFFECT> = { effect: EFFECT ->
            viewModelScope.launch {
                Napier.d("RevolverViewModel ${this@RevolverViewModel::class.simpleName} emitting effect ${effect::class.simpleName}")
                _effect.emit(effect)
            }
        }
    }

    /** Routes an incoming [event] to its registered [EventHandler], or throws if none exists. */
    private suspend fun mapEvent(event: EVENT) {
        Napier.d("RevolverViewModel ${this@RevolverViewModel::class.simpleName} received event ${event::class.simpleName}")
        if (event::class in cancellableHandlerTypes) {
            mapCancellableEvent(event)
        } else {
            mapSequentialEvent(event)
        }
    }

    /**
     * Cancels any in-flight handler of the same event type, then launches the new handler in a
     * separate coroutine and returns immediately — unblocking the event channel for the next event.
     */
    private fun mapCancellableEvent(event: EVENT) {
        activeJobs[event::class]?.cancel()
        activeJobs[event::class] = viewModelScope.launch {
            try {
                val handler = eventHandlers[event::class]
                    ?: throw IllegalStateException("the event $event was fired without a handler to handle it")
                handler(event, emitter)
            } catch (_: CancellationException) {
                // Normal cancellation from a superseding event — not an application error.
            } catch (e: Throwable) {
                Napier.w("Error caught in RevolverViewModel ${this@RevolverViewModel::class.simpleName}", e)
                mapException(e)
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause !is CancellationException) activeJobs.remove(event::class)
            }
        }
    }

    /** Original sequential behavior — suspends until the handler completes. */
    private suspend fun mapSequentialEvent(event: EVENT) {
        try {
            val handler = eventHandlers[event::class]
                ?: throw IllegalStateException("the event $event was fired without a handler to handle it")
            handler(event, emitter)
        } catch (e: Throwable) {
            Napier.w("Error caught in RevolverViewModel ${this@RevolverViewModel::class.simpleName}", e)
            mapException(e)
        }
    }

    private suspend fun mapException(e: Throwable) {
        val errorHandler = errorHandlers.firstNotNullOfOrNull { (exceptionClass, errorHandler) ->
            errorHandler.takeIf { exceptionClass.isInstance(e) }
        }

        errorHandler?.invoke(e, emitter)
    }

    /**
     * Registers a [RevolverErrorHandler] instance for exception type [ERROR].
     * Delegates to [addErrorHandler] with the handler's [RevolverErrorHandler.handleError] function.
     *
     * @param ERROR the exception type this handler responds to.
     * @param errorHandler the reusable handler implementation.
     */
    inline fun <reified ERROR : Throwable> addErrorHandler(errorHandler: RevolverErrorHandler<STATE, EFFECT, ERROR>) {
        addErrorHandler(errorHandler::handleError)
    }

    /**
     * Registers a suspend lambda as an [ErrorHandler] for exception type [ERROR].
     *
     * Handlers are matched in registration order — register more specific types before broader ones.
     * Each exception type may have at most one handler.
     *
     * @param ERROR the exception type this handler responds to; must extend [Throwable].
     * @param handler the suspend function that receives the exception and an [Emitter].
     * @throws IllegalStateException if a handler for [ERROR] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified ERROR : Throwable> addErrorHandler(noinline handler: ErrorHandler<ERROR, STATE, EFFECT>) {
        handler as? EventHandler<Throwable, STATE, EFFECT>
            ?: throw IllegalArgumentException("Error type ${ERROR::class::simpleName} must extend Throwable")

        internalErrorHandler(ERROR::class, handler)
    }

    /** @suppress Internal use only. */
    fun internalErrorHandler(type: KClass<*>, handler: EventHandler<Throwable, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one ErrorHandler can be registered for $type")
        }

        errorHandlers[type] = handler
    }

    /**
     * Registers a suspend lambda as an [EventHandler] for event type [T].
     *
     * Call this in the ViewModel's `init` block to wire up each event subtype to its handler.
     * Each event type may have at most one handler.
     *
     * ```kotlin
     * init {
     *     addEventHandler<ExampleEvent.Refresh>(::onRefresh)
     * }
     * ```
     *
     * @param T the [RevolverEvent] subtype this handler responds to.
     * @param handler the suspend function that receives the event and an [Emitter].
     * @throws IllegalStateException if a handler for [T] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EVENT> addEventHandler(noinline handler: EventHandler<T, STATE, EFFECT>) {
        handler as? EventHandler<EVENT, STATE, EFFECT>
            ?: throw IllegalArgumentException("RevolverEvent type ${T::class::simpleName} must extend EVENT")
        internalEventHandler(T::class, handler)
    }

    /**
     * Registers a cancellable [handler] for event type [T].
     *
     * Unlike [addEventHandler], when a new [T] event arrives while a previous handler for [T]
     * is still running, the previous coroutine is **cancelled** before the new one is launched.
     * The event channel is unblocked immediately — other queued events continue to be processed
     * without waiting for the previous handler to finish.
     *
     * Use this for long-running operations (network calls, heavy computation) where a newer event
     * of the same type should supersede the in-flight one — for example, switching tabs while the
     * previous tab's request is still loading.
     *
     * Exceptions are routed to registered [addErrorHandler] handlers exactly as with
     * [addEventHandler]. [CancellationException] is swallowed silently.
     *
     * @param T the [RevolverEvent] subtype this handler responds to.
     * @param handler the suspend function that receives the event and an [Emitter].
     * @throws IllegalStateException if a handler for [T] is already registered.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EVENT> addCancellableEventHandler(noinline handler: EventHandler<T, STATE, EFFECT>) {
        addEventHandler<T>(handler)
        cancellableHandlerTypes.add(T::class)
    }

    /** @suppress Internal use only. */
    fun internalEventHandler(type: KClass<*>, handler: EventHandler<EVENT, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one EventHandler can be registered for $type")
        }

        eventHandlers[type] = handler
    }

    /**
     * Sends an [event] to the ViewModel for processing. The corresponding [EventHandler] will be
     * invoked asynchronously on the ViewModel's coroutine scope.
     *
     * @throws IllegalStateException if the internal event channel cannot accept the event.
     */
    override fun emit(event: EVENT) {
        val delivered = events.trySend(event).isSuccess
        if (!delivered) {
            throw IllegalStateException("Missed event $event! You are doing something wrong during state transformation.")
        }
    }
}
