package com.umain.revolver

import app.cash.turbine.test
import com.umain.revolver.flow.CSharedFlow
import com.umain.revolver.flow.CStateFlow
import com.umain.revolver.flow.cSharedFlow
import com.umain.revolver.flow.cStateFlow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

sealed class MyRevolverState : RevolverState {
    object InitialRevolverState : MyRevolverState()
    object FirstRevolverState : MyRevolverState()
    object SecondRevolverState : MyRevolverState()

    object MyErrorRevolverState1 : MyRevolverState()
    object MyErrorRevolverState2 : MyRevolverState()
    data class GenericErrorRevolverState(val commonErrorState: CommonErrorRevolverState) : MyRevolverState()
}

sealed class CommonErrorRevolverState : RevolverState {
    object UnknownError : CommonErrorRevolverState()

    object NoNetwork : CommonErrorRevolverState()
}

sealed class MyRevolverEvent : RevolverEvent {
    object FirstRevolverEvent : MyRevolverEvent()
    object SecondRevolverEvent : MyRevolverEvent()
    object TriggerEffectRevolverEvent : MyRevolverEvent()
    data class ExceptionRevolverEvent(val exception: Throwable) : MyRevolverEvent()
}

sealed class MyRevolverEffect : RevolverEffect {
    object FirstRevolverEffect : MyRevolverEffect()
}

class Exception1 : Throwable()
class Exception2 : Throwable()
class Exception3 : Throwable()

class DefaultRevolverErrorHandler<STATE, EFFECT>(val createGenericErrorState: (commonErrorState: CommonErrorRevolverState) -> STATE) :
    RevolverErrorHandler<STATE, EFFECT, Throwable> {
    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        when (exception) {
            is Exception3 -> emit.state(createGenericErrorState(CommonErrorRevolverState.NoNetwork))
            else -> emit.state(createGenericErrorState(CommonErrorRevolverState.UnknownError))
        }
    }
}

class MyRevolverViewModel : RevolverViewModel<MyRevolverEvent, MyRevolverState, MyRevolverEffect>(
    initialState = MyRevolverState.InitialRevolverState,
) {

    init {
        addEventHandler(::onFirstEvent)
        addEventHandler(::onSecondEvent)
        addEventHandler(::onTriggerEffectEvent)
        addEventHandler(::onExceptionEvent)

        addErrorHandler { _: Exception1, emit -> emit.state(MyRevolverState.MyErrorRevolverState1) }
        addErrorHandler { _: Exception2, emit -> emit.state(MyRevolverState.MyErrorRevolverState2) }
        addErrorHandler(DefaultRevolverErrorHandler { commonErrorState -> MyRevolverState.GenericErrorRevolverState(commonErrorState) })
    }

    private fun onFirstEvent(
        event: MyRevolverEvent.FirstRevolverEvent,
        emit: Emitter<MyRevolverState, MyRevolverEffect>,
    ) {
        emit.state(MyRevolverState.FirstRevolverState)
    }

    private fun onSecondEvent(
        event: MyRevolverEvent.SecondRevolverEvent,
        emit: Emitter<MyRevolverState, MyRevolverEffect>,
    ) {
        viewModelScope.launch {
            delay(20)
            emit.state(MyRevolverState.FirstRevolverState)
            delay(20)
            emit.state(MyRevolverState.SecondRevolverState)
        }
    }

    private fun onTriggerEffectEvent(
        event: MyRevolverEvent.TriggerEffectRevolverEvent,
        emit: Emitter<MyRevolverState, MyRevolverEffect>,
    ) {
        emit.effect(MyRevolverEffect.FirstRevolverEffect)
    }

    private fun onExceptionEvent(
        event: MyRevolverEvent.ExceptionRevolverEvent,
        emit: Emitter<MyRevolverState, MyRevolverEffect>,
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
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            val state = awaitItem()

            // Then
            assertIs<MyRevolverState.InitialRevolverState>(state)
        }
    }

    @Test
    fun testFirstEventEmitsFirstState() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            viewModel.emit(MyRevolverEvent.FirstRevolverEvent)
            val initialState = awaitItem()
            val firstState = awaitItem()

            // Then
            assertIs<MyRevolverState.InitialRevolverState>(initialState)
            assertIs<MyRevolverState.FirstRevolverState>(firstState)
        }
    }

    @Test
    fun testSecondEventEmitsFirstAndSecondState() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            viewModel.emit(MyRevolverEvent.SecondRevolverEvent)
            val initialState = awaitItem()
            val firstState = awaitItem()
            val secondState = awaitItem()

            // Then
            assertIs<MyRevolverState.InitialRevolverState>(initialState)
            assertIs<MyRevolverState.FirstRevolverState>(firstState)
            assertIs<MyRevolverState.SecondRevolverState>(secondState)
        }
    }

    @Test
    fun testEffectIsEmitted() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()

        viewModel.effect.test {
            // When
            viewModel.emit(MyRevolverEvent.TriggerEffectRevolverEvent)

            // Then
            assertIs<MyRevolverEffect.FirstRevolverEffect>(awaitItem())
        }
    }

    @Test
    fun testHandlesException1() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyRevolverEvent.ExceptionRevolverEvent(Exception1()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyRevolverState.MyErrorRevolverState1, errorState)
        }
    }

    @Test
    fun testHandlesException2() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyRevolverEvent.ExceptionRevolverEvent(Exception2()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyRevolverState.MyErrorRevolverState2, errorState)
            assertNotEquals(MyRevolverState.MyErrorRevolverState1, errorState)
        }
    }

    @Test
    fun testHandlesException3() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyRevolverEvent.ExceptionRevolverEvent(Exception3()))
            val errorState = awaitItem()

            // Then
            assertEquals(MyRevolverState.GenericErrorRevolverState(CommonErrorRevolverState.NoNetwork), errorState)
        }
    }

    @Test
    fun testHandlesNonThrowable() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.state.test {
            // When
            val initialState = awaitItem()
            viewModel.emit(MyRevolverEvent.ExceptionRevolverEvent(RuntimeException("Dummy Exception")))
            val errorState = awaitItem()

            // Then
            assertEquals(MyRevolverState.GenericErrorRevolverState(CommonErrorRevolverState.UnknownError), errorState)
        }
    }

    @Test
    fun testDuplicateErrorHandlerThrows() {
        // Given
        val viewModel = MyRevolverViewModel()

        // When / Then
        assertFailsWith<IllegalStateException> {
            viewModel.addErrorHandler { _: Exception1, emit -> emit.state(MyRevolverState.FirstRevolverState) }
        }
    }

    @Test
    fun testDuplicateEventHandlerThrows() {
        // Given
        val viewModel = MyRevolverViewModel()

        // When / Then
        assertFailsWith<IllegalStateException> {
            viewModel.addEventHandler<MyRevolverEvent.FirstRevolverEvent> { _, emit ->
                emit.state(MyRevolverState.SecondRevolverState)
            }
        }
    }

    @Test
    fun testStateFlowMirrorsState() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.stateFlow.test {
            // When
            viewModel.emit(MyRevolverEvent.FirstRevolverEvent)

            // Then
            assertIs<MyRevolverState.InitialRevolverState>(awaitItem())
            assertIs<MyRevolverState.FirstRevolverState>(awaitItem())
            assertEquals(viewModel.state.value, viewModel.stateFlow.value)
        }
    }

    @Test
    fun testEffectFlowDeliversEffects() = runTest {
        // Given
        val viewModel = MyRevolverViewModel()
        viewModel.effectFlow.test {
            // When
            viewModel.emit(MyRevolverEvent.TriggerEffectRevolverEvent)

            // Then
            assertIs<MyRevolverEffect.FirstRevolverEffect>(awaitItem())
        }
    }

    @Test
    fun testPlainFlowsAreNotWrappers() {
        // Swift export can only bridge flows that are not CFlow wrapper instances.
        val viewModel = MyRevolverViewModel()
        val fake = FakeRevolverViewModel()

        assertIsNot<CStateFlow<*>>(viewModel.stateFlow)
        assertIsNot<CSharedFlow<*>>(viewModel.effectFlow)
        assertIsNot<CStateFlow<*>>(fake.stateFlow)
        assertIsNot<CSharedFlow<*>>(fake.effectFlow)
    }

    @Test
    fun testInterfaceDefaultStateFlowMirrorsState() = runTest {
        // Given
        val fake = FakeRevolverViewModel()
        fake.stateFlow.test {
            // When
            fake.emit(MyRevolverEvent.FirstRevolverEvent)

            // Then
            assertIs<MyRevolverState.InitialRevolverState>(awaitItem())
            assertIs<MyRevolverState.FirstRevolverState>(awaitItem())
        }
    }
}

private class FakeRevolverViewModel : RevolverViewModelInterface<MyRevolverEvent, MyRevolverState, MyRevolverEffect> {
    private val _state = MutableStateFlow<MyRevolverState>(MyRevolverState.InitialRevolverState)
    override val state = _state.cStateFlow()
    override val effect = MutableSharedFlow<MyRevolverEffect>().cSharedFlow()

    override fun emit(event: MyRevolverEvent) {
        _state.value = MyRevolverState.FirstRevolverState
    }
}
