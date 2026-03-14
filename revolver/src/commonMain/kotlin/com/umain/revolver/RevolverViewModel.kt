package com.umain.revolver

import com.umain.revolver.flow.cSharedFlow
import com.umain.revolver.flow.cStateFlow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Base class for implementing custom ViewModels in the Revolver framework.
 *
 * It provides a structured way to handle events, manage an immutable state,
 * and emit side effects, all within a platform-appropriate coroutine scope.
 *
 * @param EVENT The type of events this ViewModel accepts.
 * @param STATE The type of immutable states this ViewModel manages.
 * @param EFFECT The type of side effects this ViewModel triggers.
 * @property initialState The initial state the ViewModel starts with.
 */
open class RevolverViewModel<EVENT : RevolverEvent, STATE : RevolverState, EFFECT : RevolverEffect>(
    initialState: STATE,
) : RevolverViewModelInterface<EVENT, STATE, EFFECT>, BaseViewModel() {

    private val eventHandlers = mutableMapOf<KClass<*>, EventHandler<EVENT, STATE, EFFECT>>()
    private val errorHandlers = mutableMapOf<KClass<*>, ErrorHandler<Throwable, STATE, EFFECT>>()

    private val events = Channel<EVENT>(Channel.BUFFERED).also {
        it.receiveAsFlow()
            .onEach(::mapEvent)
            .launchIn(viewModelScope)
    }

    private val _state = MutableStateFlow(initialState)
    private val _effect = MutableSharedFlow<EFFECT>()

    /**
     * An observable [CStateFlow] for state changes.
     * Use this to update the UI on clients.
     */
    override val state = _state.cStateFlow(viewModelScope)

    /**
     * An observable [CSharedFlow] for side effects (e.g., navigation).
     * Effects are emitted once and are typically not cached.
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

    /**
     * Internal method to map an incoming [EVENT] to its registered [EventHandler].
     */
    private suspend fun mapEvent(event: EVENT) {
        Napier.d("RevolverViewModel ${this@RevolverViewModel::class.simpleName} received event ${event::class.simpleName}")
        try {
            val handler = eventHandlers[event::class]
                ?: throw IllegalStateException("the event $event was fired without a handler to handle it")

            handler(event, emitter)
        } catch (e: Throwable) {
            Napier.w("Error caught in RevolverViewModel ${this::class.simpleName}", e)
            mapException(e)
        }
    }

    /**
     * Internal method to process a [Throwable] using the registered [errorHandlers].
     */
    private suspend fun mapException(e: Throwable) {
        val errorHandler = errorHandlers.firstNotNullOfOrNull { (exceptionClass, errorHandler) ->
            errorHandler.takeIf { exceptionClass.isInstance(e) }
        }

        errorHandler?.invoke(e, emitter)
    }

    /**
     * Registers a reusable [RevolverErrorHandler] for a specific exception type [ERROR].
     *
     * @param ERROR The type of exception this handler handles.
     * @param errorHandler The instance of the reusable error handler.
     */
    inline fun <reified ERROR : Throwable> addErrorHandler(errorHandler: RevolverErrorHandler<STATE, EFFECT, ERROR>) {
        addErrorHandler(errorHandler::handleError)
    }

    /**
     * Registers a functional error handler for a specific exception type [ERROR].
     *
     * @param ERROR The type of exception this handler handles.
     * @param handler A lambda or function reference that processes the error.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified ERROR : Throwable> addErrorHandler(noinline handler: ErrorHandler<ERROR, STATE, EFFECT>) {
        handler as? EventHandler<Throwable, STATE, EFFECT>
            ?: throw IllegalArgumentException("Error type ${ERROR::class::simpleName} must extend Throwable")

        internalErrorHandler(ERROR::class, handler)
    }

    /**
     * Internal implementation for registering an error handler.
     */
    fun internalErrorHandler(type: KClass<*>, handler: EventHandler<Throwable, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one ErrorHandler can be registered for $type")
        }

        errorHandlers[type] = handler
    }

    /**
     * Registers an event handler for a specific [RevolverEvent] type [T].
     *
     * @param T The type of the event to handle.
     * @param handler A lambda or function reference that processes the event.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EVENT> addEventHandler(noinline handler: EventHandler<T, STATE, EFFECT>) {
        handler as? EventHandler<EVENT, STATE, EFFECT>
            ?: throw IllegalArgumentException("RevolverEvent type ${T::class::simpleName} must extend EVENT")
        internalEventHandler(T::class, handler)
    }

    /**
     * Internal implementation for registering an event handler.
     */
    fun internalEventHandler(type: KClass<*>, handler: EventHandler<EVENT, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one EventHandler can be registered for $type")
        }

        eventHandlers[type] = handler
    }

    /**
     * Used by clients to emit a new [EVENT] to the ViewModel.
     *
     * @param event The event to be processed.
     */
    override fun emit(event: EVENT) {
        val delivered = events.trySend(event).isSuccess
        if (!delivered) {
            throw IllegalStateException("Missed event $event! You are doing something wrong during state transformation.")
        }
    }
}
