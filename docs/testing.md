---
type: Guide
title: Testing
description: Unit test ViewModels in commonTest with Turbine and kotlinx-coroutines-test.
tags: [testing, turbine, coroutines]
status: stable
generated: { by: claude-code/claude-opus-5-5, at: 2026-10-14T21:23:43Z }
sources:
  - id: library-tests
    resource: https://github.com/apegroup/revolver/blob/main/revolver/src/commonTest/kotlin/com/umain/revolver/TestViewModelTests.kt
    title: Revolver's own ViewModel tests
---

# Testing

ViewModel logic lives in `commonMain`, so you can test it as plain Kotlin unit tests without an emulator, a simulator or any UI.

## Dependencies

Add these to your project's `commonTest` source set:

| Library | Use |
|---|---|
| `kotlin("test")` | `@Test`, `assertIs`, `assertEquals` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `runTest`, `StandardTestDispatcher`, `Dispatchers.setMain` |
| `app.cash.turbine:turbine` | `test { awaitItem() }` for asserting on flows |

A hand-written fake is usually enough for dependencies. Use a KMP mocking library such as Mockative if you need one.

## Setup

The ViewModel scope runs on `Dispatchers.Main`, which is read when the ViewModel is created. Install a test dispatcher before you construct the ViewModel:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTests {

    @BeforeTest
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()
}
```

## Asserting states

`state` is a `StateFlow`, so the first item Turbine receives is the **current** state:

```kotlin
@Test
fun queryChangedEmitsLoadingThenResults() = runTest {
    val viewModel = SearchViewModel(FakeSearchRepository(results = listOf("a", "b")))

    viewModel.state.test {
        viewModel.emit(SearchEvent.QueryChanged("kotlin"))

        assertIs<SearchState.Idle>(awaitItem())    // initial state
        assertIs<SearchState.Loading>(awaitItem())
        assertEquals(listOf("a", "b"), assertIs<SearchState.Results>(awaitItem()).items)
    }
}

class FakeSearchRepository(private val results: List<String>) : SearchRepository {
    override suspend fun search(query: String): List<String> {
        delay(10) // suspend so the intermediate Loading state can be observed
        return results
    }
}
```

`StateFlow` conflates values. If a handler sets two states without suspending in between, a collector can miss the first one. Either make the fake suspend, as above, or assert only on the final state.

## Asserting effects

```kotlin
@Test
fun resetEmitsConfirmation() = runTest {
    val viewModel = CounterViewModel()

    viewModel.effect.test {
        viewModel.emit(CounterEvent.Reset)
        assertIs<CounterEffect.ResetConfirmed>(awaitItem())
    }
}
```

Start collecting before you emit. Effects emitted while nothing is collecting are lost.

## Error paths

Make the fake throw, then assert the state the error handler produces:

```kotlin
@Test
fun networkErrorEmitsOfflineState() = runTest {
    val viewModel = SearchViewModel(FailingSearchRepository(NetworkException()))

    viewModel.state.test {
        viewModel.emit(SearchEvent.QueryChanged("kotlin"))

        assertIs<SearchState.Idle>(awaitItem())
        assertIs<SearchState.Loading>(awaitItem())
        assertIs<SearchState.Offline>(awaitItem())
    }
}

class FailingSearchRepository(private val error: Throwable) : SearchRepository {
    override suspend fun search(query: String): List<String> {
        delay(10)
        throw error
    }
}
```

## Starting from a specific state

`initialState` is a constructor parameter of `RevolverViewModel`. If your ViewModel forwards it, tests can start from any state without first running other handlers:

```kotlin
class SearchViewModel(
    private val repository: SearchRepository,
    initialState: SearchState = SearchState.Idle,
) : RevolverViewModel<SearchEvent, SearchState, SearchEffect>(initialState)
```

Revolver's own test suite has more examples.[^library-tests]

[^library-tests]: Revolver's own ViewModel tests
