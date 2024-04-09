<p align="center"><img src="https://raw.githubusercontent.com/apegroup/revolver/main/assets/logo.svg" width="150"/></p>
<h1 align="center"> Revolver</h1>
<p align="center"> Immutable event based state management framework</p>


<br><br/>
## how it works

Revolver is a Kotlin Multiplatform state management solution that enforces one single immutable state. Information is passed from clients to KMM by emitting `RevolverEvents` to a `RevolverViewModel`. This `RevolverViewModel` will have readonly `RevolverState` and `RevolverEffect` flows that the clients can subscribe to for updates.

The first thing we need are our sealed classes responsible for communicating data to and from kmm:

```Kotlin
sealed class ExampleEvent : RevolverEvent {
    object Refresh : ExampleEvent()
}
```

```Kotlin
sealed class ExampleState : RevolverState {
    class Loading : ExampleState()
    data class Loaded(val result: String) : ExampleState()
}
```
Avoid using `object` for sealed classes it might cause problems with state updates.

```Kotlin
sealed class ExampleEffect : RevolverEffect {
    data class ShowToast(val message: String) : ExampleEffect()
}
```

with these 3 in place, we can create our own `RevolverViewModel` implementation. This `RevolverViewModel` always needs an initial state since all event handling happens asynchroniously.

```Kotlin
class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(
    initialState = ExampleState.Loading,
) 
```
In this ViewModel we can register one or more `EventHandlers` that are responsible for mapping an incomming event to one or more states.

```Kotlin
class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(
    initialState = ExampleState.Loading,
) {

    init {
        addEventHandler<ExampleEvent.Refresh>(::onRefresh)
    }

    suspend fun onRefresh(
        event: DealsEvent.Refresh,
        emit: Emitter<ExampleState, ExampleEffect>,
    ) {
        emit.state(ExampleState.Loading)
        val data = someDataFetchingOperation()
        
        emit.state(ExampleState.Loaded(data))
    }
}
```
As you can see, these event handlers have an `Emitter` used for emitting `RevolverState` changes or side `RevolverEffect` to the clients.

With these 4 things you have your basic state handling flow set up.
<br><br/>
## Error handling

A very important consept is that we don't want to expose any Kotlin Multiplatform errors to the clients directly. Therefore the `RevolverViewModel` will catch all exceptions that bubble up and allows you to handle them similarly to any other `RevolverEvent`. In every `RevolverViewModel` you should at least register one `ErrorHandler`.

If you want a quick solution you can register one event handler that catches the generic `Exception`. but you probably want to define multiple error handlers that catch your custom Errors

```kotlin
class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(ExampleState.Loading) {

    init {
        addErrorHandler<IllegalStateException>(::onIllegalStateException)
        addErrorHandler<Exception>(::onException)
    }
    
    private fun onIllegalStateException(
        exception: IllegalStateException,
        emit: Emitter<ExampleState, ExampleEffect>,
    ) {
       // handle this specific error case
    }

    private fun onException(
        exception: Exception,
        emit: Emitter<ExampleState, ExampleEffect>,
    ) {
        // handle generic Exceptions
    }
}
```
Keep in mind that the order of handler registration matters, it wil match a thrown error in the order of handler registration, so if one exception type extends another, make sure the child type is registered first.

### Reusing error handlers

It is possible that you don't want to rewrite the same error handling implementation for every viewModel, for example a toast should pop up every time a `NoConnectionException` is thrown.

To do this Revolver supports reusable error handling classes. If you don't want to worry about error handling you can use Revolvers build in error handling class to map any exception directly to a state.
```kotlin
class ExampleViewModel : RevolverViewModel<ExampleEvent, ExampleState, ExampleEffect>(ExampleState.Loading) {

    init {
        addErrorHandler(RevolverDefaultErrorHandler(ExampleState.Error))
    }
}
```

this will result in any exception thrown to emit an `ExampleState.Error`.

ofcourse you can implement your own `RevolverErrorHandler` to do more complex state mapping.
```kotlin
class ExampleErrorHandler<STATE, EFFECT> : RevolverErrorHandler<STATE, EFFECT, Throwable> {

    override suspend fun handleError(exception: Throwable, emit: Emitter<STATE, EFFECT>) {
        // log error, call some other external methods, map to reusable states ...
    }
}

```
which you can then register in your viewmodel like any other reused error handler.

<br><br/>
## Testing

if you implement your viewmodels as described above, you will end up with a completly immutable and defined state machine where all actions and responses are mapped. The great thing this allows us to do is test the complete state flow in Kotlin multiplatform unit tests without needing any link to a client application

There are a couple external tools we recommend when writing tests:
* [Turbine](https://github.com/cashapp/turbine)
* [Mockative](https://github.com/mockative/mockative)
* kotlin.test
* kotlinx.coroutines.test

using these it's really easy to test your state machine's flow, take this example viewmodel
```Kotlin
sealed class ExampleEvent : RevolverEvent {
    object Refresh : ExampleEvent()
}

sealed class ExampleState : RevolverState {
    object Loading : ExampleState()
    data class Loaded(val data: String) : ExampleState()
}

class ExampleViewModel(
    private val repository: ExampleRepository,
    initialState: ExampleState = ExampleState.Loading,
) : RevolverViewModel<ExampleEvent, ExampleState, EFFECT>(initialState) {

    init {
        addEventHandler<ExampleEvent.Refresh>(::onRefresh)
    }

    private suspend fun onRefresh(
        event: ExampleEvent.Refresh,
        emit: Emitter<ExampleState, EFFECT>,
    ) {
        emit.state(ExampleState.Loading)

        val result = repository.fetchData()
        emit.state(ExampleState.Loaded(data))
    }
}
```


```Kotlin

@OptIn(ExperimentalCoroutinesApi::class)
internal class ExampleViewModelTests {

    @Mock
    private val exampleRepository = mock(classOf<ExampleRepository>())


    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun dispose() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun onRefreshEmitsLoadingAndLoadedState() = runTest {
        // given
        given(exampleRepository).coroutine { fetchData() }.thenReturn("testData")
        
        val initialState = ExampleState.Loading
        val viewmodel = ExampleViewModel(exampleRepository, initialState)
        
        viewmodel.state.test {
            
            // when
            viewmodel.emit(ExampleEvent.Refresh)
            
            // then
            assertIs<ExampleState.Loading>(awaitItem())
            val loadedState = assertIs<ExampleState.Loaded>(awaitItem())
            assertEquals("testData", loadedState.data)
        }
        
    }
}
```
as you can see we use Mockative to mock our repository, so we can purely focus on testing the viewmodel. Then we use Turbine to test the `viewmodel.state` (you can also test `viewmodel.effect`).

## Using in your project
to use this package in your kotlin multiplatform project you need to add our gradle server to you build.gradle.kts file:
```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/apegroup/revolver/")

            credentials {
                username = System.getenv("GH_USERNAME") ?: ""
                password = System.getenv("GH_TOKEN") ?: ""
            }
        }
    }
}
```
Then you can import it in your commonMain just like any other Kotlin multiplatform package

```kotlin
implementation("com.umain:revolver:{LATEST_VERSION}")
```

## Contribution

This package is very much still in experimental mode and using this in a production environment is at your own risk. Bug reports, feature requests, or contributions are very much appreciated!
