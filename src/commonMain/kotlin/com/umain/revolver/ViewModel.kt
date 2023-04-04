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
 * Extendable class for implementing custom ViewModels. requires types
 * for its Event, State, and Effect implementations e.g.
 * ```
 * class ExampleViewModel : ViewModel<ExampleEvent, ExampleState, ExampleEffect>(
 *    initialState: ExampleState.SomeState
 * )
 * ```
 */
open class ViewModel<EVENT : Event, STATE : State, EFFECT : Effect>(
    initialState: STATE,
) : ViewModelInterface<EVENT, STATE, EFFECT>, BaseViewModel() {

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
     * StateFlow for observing state changes
     */
    override val state = _state.cStateFlow()

    /**
     * SharedFlow for observing side effects. Used for one of events like "Move to the next screen"
     */
    override val effect = _effect.cSharedFlow()

    private val emitter: Emitter<STATE, EFFECT> = object : Emitter<STATE, EFFECT> {
        override val state: StateEmitter<STATE> = { state: STATE ->
            viewModelScope.launch {
                Napier.d("ViewModel ${this@ViewModel::class.simpleName} emitting state ${state::class.simpleName}")
                _state.emit(state)
            }
        }
        override val effect: EffectEmitter<EFFECT> = { effect: EFFECT ->
            viewModelScope.launch {
                Napier.d("ViewModel ${this@ViewModel::class.simpleName} emitting effect ${effect::class.simpleName}")
                _effect.emit(effect)
            }
        }
    }

    /**
     * Calls the corresponding [EventHandler] for the added [Event]
     */
    private suspend fun mapEvent(event: EVENT) {
        Napier.d("ViewModel ${this@ViewModel::class.simpleName} received event ${event::class.simpleName}")
        try {
            val handler = eventHandlers[event::class]
                ?: throw IllegalStateException("the event $event was fired without a handler to handle it")

            handler(event, emitter)
        } catch (e: Throwable) {
            Napier.w("Error caught in ViewModel ${this::class.simpleName}", e)
            mapException(e)
        }
    }

    private suspend fun mapException(e: Throwable) {
        val errorHandler = errorHandlers.firstNotNullOfOrNull { (exceptionClass, errorHandler) ->
            errorHandler.takeIf { exceptionClass.isInstance(e) }
        }

        errorHandler?.invoke(e, emitter)
    }

    inline fun <reified ERROR : Throwable> addErrorHandler(errorHandler: MviErrorHandler<STATE, EFFECT, ERROR>) {
        addErrorHandler(errorHandler::handleError)
    }

    /**
     * Adds an [ErrorHandler] to the list of handlers in this ViewModel. Only used internally in KMM.
     * Each [Error] type can only have one handler.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified ERROR : Throwable> addErrorHandler(noinline handler: ErrorHandler<ERROR, STATE, EFFECT>) {
        handler as? EventHandler<Throwable, STATE, EFFECT>
            ?: throw IllegalArgumentException("Error type ${ERROR::class::simpleName} must extend Throwable")

        internalErrorHandler(ERROR::class, handler)
    }

    fun internalErrorHandler(type: KClass<*>, handler: EventHandler<Throwable, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one ErrorHandler can be registered for $type")
        }

        errorHandlers[type] = handler
    }

    /**
     * Registers an [EventHandler] in this ViewModel. Only used internally in KMM.
     * Each [Event] type can only have one handler.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EVENT> addEventHandler(noinline handler: EventHandler<T, STATE, EFFECT>) {
        handler as? EventHandler<EVENT, STATE, EFFECT>
            ?: throw IllegalArgumentException("Event type ${T::class::simpleName} must extend EVENT")
        internalEventHandler(T::class, handler)
    }

    fun internalEventHandler(type: KClass<*>, handler: EventHandler<EVENT, STATE, EFFECT>) {
        if (eventHandlers.containsKey(type)) {
            throw IllegalStateException("only one EventHandler can be registered for $type")
        }

        eventHandlers[type] = handler
    }

    /**
     * Used by clients to emit a new event to the ViewModel
     */
    override fun emit(event: EVENT) {
        val delivered = events.trySend(event).isSuccess
        if (!delivered) {
            throw IllegalStateException("Missed event $event! You are doing something wrong during state transformation.")
        }
    }
}
