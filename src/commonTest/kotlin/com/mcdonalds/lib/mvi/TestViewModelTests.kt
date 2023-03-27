package com.mcdonalds.lib.mvi

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

sealed class MyState : State {
    object InitialState : MyState()
    object FirstState : MyState()
    object SecondState : MyState()

    object MyErrorState1 : MyState()
    object MyErrorState2 : MyState()
    data class GenericErrorState(val commonErrorState: CommonErrorState) : MyState()
}

sealed class CommonErrorState : State {
    object UnknownError : CommonErrorState()

    object NoNetwork : CommonErrorState()
}

sealed class MyEvent : Event {
    object FirstEvent : MyEvent()
    object SecondEvent : MyEvent()
    object TriggerEffectEvent : MyEvent()
    data class ExceptionEvent(val exception: Exception) : MyEvent()
}

sealed class MyEffect : Effect {
    object FirstEffect : MyEffect()
}

class Exception1 : MviException()
class Exception2 : MviException()
class Exception3 : MviException()

class DefaultMviErrorHandler<STATE, EFFECT>(val createGenericErrorState: (commonErrorState: CommonErrorState) -> STATE) :
    MviErrorHandler<STATE, EFFECT, MviException> {
    override suspend fun handleError(exception: MviException, emit: Emitter<STATE, EFFECT>) {
        when (exception) {
            is Exception3 -> emit.state(createGenericErrorState(CommonErrorState.NoNetwork))
            else -> emit.state(createGenericErrorState(CommonErrorState.UnknownError))
        }
    }
}

class MyViewModel : ViewModel<MyEvent, MyState, MyEffect>(
    initialState = MyState.InitialState,
) {

    init {
        addEventHandler(::onFirstEvent)
        addEventHandler(::onSecondEvent)
        addEventHandler(::onTriggerEffectEvent)
        addEventHandler(::onExceptionEvent)

        addErrorHandler { _: Exception1, emit -> emit.state(MyState.MyErrorState1) }
        addErrorHandler { _: Exception2, emit -> emit.state(MyState.MyErrorState2) }
        addErrorHandler(DefaultMviErrorHandler { commonErrorState -> MyState.GenericErrorState(commonErrorState) })
    }

    private fun onFirstEvent(
        event: MyEvent.FirstEvent,
        emit: Emitter<MyState, MyEffect>,
    ) {
        emit.state(MyState.FirstState)
    }

    private fun onSecondEvent(
        event: MyEvent.SecondEvent,
        emit: Emitter<MyState, MyEffect>,
    ) {
        viewModelScope.launch {
            delay(20)
            emit.state(MyState.FirstState)
            delay(20)
            emit.state(MyState.SecondState)
        }
    }

    private fun onTriggerEffectEvent(
        event: MyEvent.TriggerEffectEvent,
        emit: Emitter<MyState, MyEffect>,
    ) {
        emit.effect(MyEffect.FirstEffect)
    }

    private fun onExceptionEvent(
        event: MyEvent.ExceptionEvent,
        emit: Emitter<MyState, MyEffect>,
    ) {
        throw event.exception
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTests {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun testInitialStateIsCorrect() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            val state = awaitItem()

            // Then
            assertIs<MyState.InitialState>(state)
        }
    }

    @Test
    fun testFirstEventEmitsFirstState() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            viewModel.emit(MyEvent.FirstEvent)
            val initialState = awaitItem()
            val firstState = awaitItem()

            // Then
            assertIs<MyState.InitialState>(initialState)
            assertIs<MyState.FirstState>(firstState)
        }
    }

    @Test
    fun testSecondEventEmitsFirstAndSecondState() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            viewModel.emit(MyEvent.SecondEvent)
            val initialState = awaitItem()
            val firstState = awaitItem()
            val secondState = awaitItem()

            // Then
            assertIs<MyState.InitialState>(initialState)
            assertIs<MyState.FirstState>(firstState)
            assertIs<MyState.SecondState>(secondState)
        }
    }

    @Test
    fun testEffectIsEmitted() = runTest {
        // Given
        val viewModel = MyViewModel()

        viewModel.effect.test {
            // When
            viewModel.emit(MyEvent.TriggerEffectEvent)

            // Then
            assertIs<MyEffect.FirstEffect>(awaitItem())
        }
    }

    @Test
    fun testHandlesException1() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyEvent.ExceptionEvent(Exception1()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyState.MyErrorState1, errorState)
        }
    }

    @Test
    fun testHandlesException2() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyEvent.ExceptionEvent(Exception2()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyState.MyErrorState2, errorState)
            assertNotEquals(MyState.MyErrorState1, errorState)
        }
    }

    @Test
    fun testHandlesException3() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyEvent.ExceptionEvent(Exception3()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyState.GenericErrorState(CommonErrorState.NoNetwork), errorState)
        }
    }

    @Test
    fun testHandlesNonMviException() = runTest {
        // Given
        val viewModel = MyViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyEvent.ExceptionEvent(RuntimeException("Dummy Exception")))
            val errorState = awaitItem()

            // Then
            assertEquals(MyState.GenericErrorState(CommonErrorState.UnknownError), errorState)
        }
    }
}
