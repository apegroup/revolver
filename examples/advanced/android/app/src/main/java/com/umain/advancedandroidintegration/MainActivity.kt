package com.umain.advancedandroidintegration

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umain.advancedandroidintegration.presentation.MainViewEffect
import com.umain.advancedandroidintegration.presentation.MainViewEvent
import com.umain.advancedandroidintegration.presentation.MainViewState
import com.umain.advancedandroidintegration.presentation.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var uiState by remember { mutableStateOf<MainViewState>(MainViewState.Loading) }

                // Collect state updates and one-shot effects.
                LaunchedEffect(Unit) {
                    viewModel.emit(MainViewEvent.LoadItems)

                    viewModel.state.collect { state ->
                        uiState = state
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is MainViewEffect.ShowToast ->
                                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Revolver — Advanced Example") })
                    },
                ) { padding ->
                    MainContent(
                        state = uiState,
                        onRefresh = { viewModel.emit(MainViewEvent.LoadItems) },
                        onRetry = { viewModel.emit(MainViewEvent.RetryAfterError) },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    state: MainViewState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is MainViewState.Loading -> {
                CircularProgressIndicator()
            }

            is MainViewState.Loaded -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text("Refresh")
                    }
                }
            }

            is MainViewState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )

                    if (state.retryable) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
