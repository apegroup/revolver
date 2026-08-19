---
layout: default
title: Testing
nav_order: 5
description: "Unit testing ViewModels with Turbine and Mockative."
---

# Testing

Because all business logic lives in `commonMain` and states are immutable value types, Revolver ViewModels can be tested as pure Kotlin unit tests — no Android emulator, no iOS simulator, no UI harness needed.

---

## Tools

All testing dependencies are already included in `commonTest`:

| Library | Purpose |
|---|---|
| [Turbine](https://github.com/cashapp/turbine) | Assertion DSL for Kotlin Flows |
| [Mockative](https://github.com/mockative/mockative) | KMP-compatible mocking via `@Mock` |
| `kotlin.test` | `@Test`, `assertIs`, `assertEquals`, `assertFailsWith` |
| `kotlinx.coroutines.test` | `runTest`, `StandardTestDispatcher`, `Dispatchers.setMain` |

---

## Test class setup

Replace the main dispatcher with a `StandardTestDispatcher` so coroutines are deterministic:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
internal class MyViewModelTests {

    @Mock
    private val repository = mock(classOf<MyRepository>())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }
}
```

---

## Testing state flows

Use Turbine's `test { }` block to assert each emitted state in order.

```kotlin
@Test
fun loadEmitsLoadingThenResults() = runTest {
    given(repository).coroutine { fetchItems() }.thenReturn(listOf("a", "b"))

    val viewModel = MyViewModel(repository)

    viewModel.state.test {
        viewModel.emit(MyEvent.Load)

        assertIs<MyState.Loading>(awaitItem())
        val results = assertIs<MyState.Results>(awaitItem())
        assertEquals(listOf("a", "b"), results.items)
    }
}
```

> **Tip**: `awaitItem()` suspends until the next item is emitted or the test times out. The default timeout is 1 second; increase it with `test(timeout = 5.seconds) { }` for slow operations.

---

## Testing effects

The `effect` flow is tested the same way. Combine with state assertions when needed:

```kotlin
@Test
fun submitEmitsSuccessEffect() = runTest {
    given(repository).coroutine { submit(any()) }.thenReturn(Unit)

    val viewModel = MyViewModel(repository)

    viewModel.effect.test {
        viewModel.emit(MyEvent.Submit("payload"))

        val effect = assertIs<MyEffect.ShowSuccess>(awaitItem())
        assertEquals("Done!", effect.message)
    }
}
```

---

## Testing error handling

Throw from the mock to exercise error paths:

```kotlin
@Test
fun loadNetworkErrorEmitsOfflineState() = runTest {
    given(repository).coroutine { fetchItems() }.thenThrow(NetworkException())

    val viewModel = MyViewModel(repository)

    viewModel.state.test {
        viewModel.emit(MyEvent.Load)

        assertIs<MyState.Loading>(awaitItem())
        assertIs<MyState.Offline>(awaitItem())
    }
}
```

---

## Testing intermediate states

When a handler emits multiple states (e.g. `Loading` → `Loaded`), assert them all in order:

```kotlin
@Test
fun refreshEmitsLoadingThenLoaded() = runTest {
    given(repository).coroutine { fetchData() }.thenReturn("result")

    val viewModel = MyViewModel(repository)

    viewModel.state.test {
        viewModel.emit(MyEvent.Refresh)

        assertIs<MyState.Loading>(awaitItem())       // first emission
        val loaded = assertIs<MyState.Loaded>(awaitItem()) // second emission
        assertEquals("result", loaded.data)
    }
}
```

---

## Overriding the initial state

`RevolverViewModel` takes an `initialState` constructor parameter. Pass it directly in tests to start from a known state without triggering any handler:

```kotlin
val viewModel = MyViewModel(
    repository = repository,
    initialState = MyState.Results(listOf("existing")),
)
```

This pattern is useful when testing handlers that depend on the current state value.

---

## Overriding the coroutine scope

`CreateViewModelScope` can be replaced for finer-grained coroutine control in tests. The test dispatcher installed via `Dispatchers.setMain` is picked up automatically when using `StandardTestDispatcher`.

---

## Complete example

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchViewModelTests {

    @Mock
    private val repository = mock(classOf<SearchRepository>())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun queryChangedEmitsLoadingThenResults() = runTest {
        given(repository).coroutine { search("kotlin") }.thenReturn(listOf("Kotlin Docs", "KMP Guide"))

        val viewModel = SearchViewModel(repository)

        viewModel.state.test {
            viewModel.emit(SearchEvent.QueryChanged("kotlin"))

            assertIs<SearchState.Loading>(awaitItem())
            val results = assertIs<SearchState.Results>(awaitItem())
            assertEquals(2, results.items.size)
        }
    }

    @Test
    fun clearResultsRestoresIdleState() = runTest {
        val viewModel = SearchViewModel(repository, initialState = SearchState.Results(listOf("item")))

        viewModel.state.test {
            viewModel.emit(SearchEvent.ClearResults)

            assertIs<SearchState.Idle>(awaitItem())
        }
    }

    @Test
    fun networkErrorEmitsOfflineState() = runTest {
        given(repository).coroutine { search(any()) }.thenThrow(NetworkException())

        val viewModel = SearchViewModel(repository)

        viewModel.state.test {
            viewModel.emit(SearchEvent.QueryChanged("kotlin"))

            assertIs<SearchState.Loading>(awaitItem())
            assertIs<SearchState.Offline>(awaitItem())
        }
    }
}
```
