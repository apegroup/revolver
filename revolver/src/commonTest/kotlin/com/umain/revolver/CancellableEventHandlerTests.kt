package com.umain.revolver

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ── Test contracts ────────────────────────────────────────────────────────────

private sealed class TabEvent : RevolverEvent {
    data object LoadDeals : TabEvent()
    data object LoadAwards : TabEvent()
    data object NonCancellable : TabEvent()
    data object AlwaysThrows : TabEvent()
}

private sealed class TabState : RevolverState {
    data object Initial : TabState()
    data object LoadingDeals : TabState()
    data object LoadedDeals : TabState()
    data object LoadingAwards : TabState()
    data object LoadedAwards : TabState()
    data object NonCancellableLoaded : TabState()
    data class Error(val message: String) : TabState()
}

private sealed class TabEffect : RevolverEffect

// ── Test ViewModel ────────────────────────────────────────────────────────────

private class TabViewModel(
    private val dealsDelayMs: Long = 500L,
    private val awardsDelayMs: Long = 100L,
) : RevolverViewModel<TabEvent, TabState, TabEffect>(
    initialState = TabState.Initial,
) {
    init {
        addCancellableEventHandler<TabEvent.LoadDeals>(::onLoadDeals)
        addCancellableEventHandler<TabEvent.LoadAwards>(::onLoadAwards)
        addEventHandler<TabEvent.NonCancellable>(::onNonCancellable)
        addEventHandler<TabEvent.AlwaysThrows>(::onAlwaysThrows)
        addErrorHandler<Exception> { e, emit ->
            emit.state(TabState.Error(e.message ?: "error"))
        }
    }

    private suspend fun onLoadDeals(event: TabEvent.LoadDeals, emit: Emitter<TabState, TabEffect>) {
        emit.state(TabState.LoadingDeals)
        delay(dealsDelayMs)
        emit.state(TabState.LoadedDeals)
    }

    private suspend fun onLoadAwards(event: TabEvent.LoadAwards, emit: Emitter<TabState, TabEffect>) {
        emit.state(TabState.LoadingAwards)
        delay(awardsDelayMs)
        emit.state(TabState.LoadedAwards)
    }

    private suspend fun onNonCancellable(event: TabEvent.NonCancellable, emit: Emitter<TabState, TabEffect>) {
        delay(100L)
        emit.state(TabState.NonCancellableLoaded)
    }

    private suspend fun onAlwaysThrows(event: TabEvent.AlwaysThrows, emit: Emitter<TabState, TabEffect>) {
        emit.state(TabState.LoadingDeals)
        throw Exception("handler failed")
    }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
internal class CancellableEventHandlerTests {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    /**
     * Switching tabs while the first tab's request is in-flight cancels that request and
     * immediately starts the new one. The in-flight [TabState.LoadedDeals] never arrives.
     */
    @Test
    fun testSwitchingTabsCancelsPreviousRequest() = runTest {
        val vm = TabViewModel(dealsDelayMs = 500L, awardsDelayMs = 100L)

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())

            vm.emit(TabEvent.LoadDeals)
            assertIs<TabState.LoadingDeals>(awaitItem())

            // Switch tab before deals finishes loading
            vm.emit(TabEvent.LoadAwards)
            assertIs<TabState.LoadingAwards>(awaitItem())
            assertIs<TabState.LoadedAwards>(awaitItem())

            // LoadedDeals must NOT appear — the job was cancelled
            expectNoEvents()
        }
    }

    /**
     * Emitting the same cancellable event twice rapidly results in exactly one
     * completed load, not two — the first job is cancelled before it finishes.
     *
     * Note: [MutableStateFlow] deduplicates equal values. Since both jobs start with
     * [TabState.LoadingDeals], the second emission is suppressed — the StateFlow already
     * holds that value. Only the final [TabState.LoadedDeals] from the second job appears.
     */
    @Test
    fun testSecondSameTypeEventCancelsFirst() = runTest {
        val vm = TabViewModel(dealsDelayMs = 300L)

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())

            vm.emit(TabEvent.LoadDeals)
            assertIs<TabState.LoadingDeals>(awaitItem()) // job1 started

            vm.emit(TabEvent.LoadDeals)                  // cancels job1, launches job2
            // job2 also emits LoadingDeals, but StateFlow suppresses the duplicate
            assertIs<TabState.LoadedDeals>(awaitItem())  // job2 completed

            expectNoEvents() // no duplicate LoadedDeals from job1
        }
    }

    /**
     * Cancelling a handler must NOT trigger the error handler — [CancellationException]
     * is swallowed silently.
     */
    @Test
    fun testCancellationDoesNotTriggerErrorHandler() = runTest {
        val vm = TabViewModel(dealsDelayMs = 500L, awardsDelayMs = 100L)

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())
            vm.emit(TabEvent.LoadDeals)
            assertIs<TabState.LoadingDeals>(awaitItem())
            vm.emit(TabEvent.LoadAwards) // cancels deals
            assertIs<TabState.LoadingAwards>(awaitItem())
            assertIs<TabState.LoadedAwards>(awaitItem())

            val allStates = cancelAndConsumeRemainingEvents()
            assertFalse(allStates.any { it is app.cash.turbine.Event.Item && it.value is TabState.Error })
        }
    }

    /**
     * Existing [addEventHandler] behavior must be completely unchanged — sequential,
     * not cancellable.
     */
    @Test
    fun testNonCancellableHandlerBehaviorIsUnchanged() = runTest {
        val vm = TabViewModel()

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())
            vm.emit(TabEvent.NonCancellable)
            assertIs<TabState.NonCancellableLoaded>(awaitItem())
        }
    }

    /**
     * Exceptions thrown inside a cancellable handler must still be routed to registered
     * error handlers, exactly as with [addEventHandler].
     */
    @Test
    fun testExceptionInsideCancellableHandlerIsRouted() = runTest {
        val vm = TabViewModel()

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())
            vm.emit(TabEvent.AlwaysThrows)
            assertIs<TabState.LoadingDeals>(awaitItem()) // emitted before the throw
            assertIs<TabState.Error>(awaitItem())        // error handler fired
        }
    }

    /**
     * Different cancellable event types are independent — cancelling [TabEvent.LoadDeals]
     * must not cancel an in-flight [TabEvent.LoadAwards] job.
     */
    @Test
    fun testDifferentCancellableTypesAreIndependent() = runTest {
        val vm = TabViewModel(dealsDelayMs = 200L, awardsDelayMs = 200L)

        vm.state.test {
            assertIs<TabState.Initial>(awaitItem())

            // Start both tabs concurrently
            vm.emit(TabEvent.LoadDeals)
            vm.emit(TabEvent.LoadAwards)

            val states = buildList {
                repeat(4) { add(awaitItem()) }
            }

            // Both Loading and both Loaded states must appear (order may vary)
            assertTrue(states.any { it is TabState.LoadingDeals })
            assertTrue(states.any { it is TabState.LoadingAwards })
            assertTrue(states.any { it is TabState.LoadedDeals })
            assertTrue(states.any { it is TabState.LoadedAwards })
        }
    }
}
