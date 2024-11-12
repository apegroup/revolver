package com.umain.basicandroidintegration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.umain.basicandroidintegration.presentation.MainViewEvent
import com.umain.basicandroidintegration.presentation.MainViewModel
import com.umain.basicandroidintegration.presentation.MainViewState
import com.umain.basicandroidintegration.ui.theme.BasicAndroidIntegrationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val viewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BasicAndroidIntegrationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Content(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun Content(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    // Local UI state that tracks ViewModel's current state
    val uiState = remember { mutableStateOf(viewModel.state.value) }

    LaunchedEffect(key1 = true) {
        /*
            At this point, the view is initialized and ready, so we notify the ViewModel.
            The ViewModel can start fetching data (e.g., from network, database, etc.).

            Initially, the ViewModel's state is set to MainViewState.Loading, so the user
            will see the corresponding UI content for Loading until data is ready.

            Once data is loaded or if an error occurs, the ViewModel will emit MainViewState.Loaded
            or MainViewState.Error respectively, updating the UI.
        */
        viewModel.emit(MainViewEvent.ViewReady("show something"))

        /*
            Collects state updates from the ViewModel, ensuring the UI reflects state changes.
            Without this collection, the view would stay in the initial MainViewState.Loading
            state indefinitely.
        */
        launch {
            viewModel.state.collect {
                uiState.value = it
            }
        }
    }

    // Renders content based on the current UI state
    when (uiState.value) {
        is MainViewState.Error -> {
            val value = uiState.value as MainViewState.Error
            Text(
                text = value.errorMessage,
                color = Color.Red,
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 100.dp)
            )
        }

        is MainViewState.Loading -> {
            Row(
                modifier = modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is MainViewState.Loaded -> {
            val value = uiState.value as MainViewState.Loaded
            Text(
                text = value.welcomeMessage,
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 100.dp)
            )
        }
    }
}
