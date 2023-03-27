package com.mcdonalds.lib.mvi

import com.mcdonalds.lib.mvi.flow.cSharedFlow
import com.mcdonalds.lib.mvi.flow.cStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * NB! If you change the order of the generics or rename this class
 * you will break the moko-kswift generation of the ViewModel extensions
 * for an observable state in iOS.
 */
open class ViewModel<EVENT : Event, STATE : State, EFFECT : Effect>(
    initialState: STATE,
) : ViewModelInterface<EVENT, STATE, EFFECT>, BaseViewModel() {

    private val handlers = mutableMapOf<String, EventHandler<EVENT, STATE, EFFECT>>()
    val errorHandlers = mutableMapOf<KClass<*>, ErrorHandler<*, STATE, EFFECT>>()
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
                println("ViewModel ${this@ViewModel::class.simpleName} emitting state ${state::class.simpleName}")
                _state.emit(state)
            }
        }
        override val effect: EffectEmitter<EFFECT> = { effect: EFFECT ->
            viewModelScope.launch {
                println("ViewModel ${this@ViewModel::class.simpleName} emitting effect ${effect::class.simpleName}")
                _effect.emit(effect)
            }
        }
    }

    /**
     * Calls the corresponding [EventHandler] for the added [Event]
     */
    private suspend fun mapEvent(event: EVENT) {
        println("ViewModel ${this@ViewModel::class.simpleName} received event ${event::class.simpleName}")
        try {
            val eventType = event::class.simpleName
                ?: throw IllegalArgumentException("Anonymous objects not supported")
            val handler = handlers[eventType]
                ?: throw IllegalStateException("the event $event was fired without a handler to handle it")
            handler(event, emitter)
        } catch (e: Throwable) {
            println("Error caught in ViewModel ${this::class.simpleName}\n\n$e")
            mapException(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun mapException(e: Throwable) {
        val kmmException = e as? MviException ?: MviException(e)

        val errorHandler = errorHandlers.firstNotNullOfOrNull { (exceptionClass, errorHandler) ->
            errorHandler.takeIf {
                exceptionClass.isInstance(kmmException)
            }
        } as? ErrorHandler<MviException, STATE, EFFECT>

        errorHandler?.invoke(kmmException, emitter)
    }

    inline fun <reified ERROR : MviException> addErrorHandler(errorHandler: MviErrorHandler<STATE, EFFECT, ERROR>) {
        addErrorHandler(errorHandler::handleError)
    }

    inline fun <reified ERROR : MviException> addErrorHandler(noinline handler: ErrorHandler<ERROR, STATE, EFFECT>) {
        errorHandlers[ERROR::class] = handler
    }

    /**
     * Registers an [EventHandler] in this ViewModel. Only used internally in KMM.
     * Each [Event] implementation can only have one handler.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : EVENT> addEventHandler(noinline handler: EventHandler<T, STATE, EFFECT>) {
        handler as? EventHandler<EVENT, STATE, EFFECT>
            ?: throw IllegalArgumentException("Event handler must have the right type")
        val name = T::class.simpleName
            ?: throw IllegalArgumentException("Anonymous objects not supported as type")
        internalEventHandler(name, handler)
    }

    /**
     * Adds an [EventHandler] to the list of handlers in this ViewModel. Only used internally in KMM.
     */
    fun internalEventHandler(name: String, handler: EventHandler<EVENT, STATE, EFFECT>) {
        if (handlers.containsKey(name)) {
            throw IllegalStateException("only one handler per event can be registered for $name")
        }

        handlers[name] = handler
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
