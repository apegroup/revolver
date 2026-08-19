package com.umain.mediumandroidintegration.search

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Search screen: search bar, paginated results list, load-more, and error recovery —
 * all driven by [SearchViewModel].
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.state.collect { uiState = it }
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SearchEffect.ScrollToTop -> listState.animateScrollToItem(0)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            onSearch = { query -> viewModel.emit(SearchEvent.Search(query)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when (val s = uiState) {
            SearchState.Idle -> IdleHint()
            SearchState.InitialLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            is SearchState.Error -> ErrorView(
                message = s.message,
                onRetry = { Toast.makeText(context, "Re-submit your query above", Toast.LENGTH_SHORT).show() },
            )
            is SearchState.Results -> ResultsList(
                state = s,
                listState = listState,
                onLoadMore = { viewModel.emit(SearchEvent.LoadMore) },
            )
        }
    }
}

@Composable
private fun SearchBar(onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search…") },
        trailingIcon = {
            IconButton(onClick = { onSearch(query) }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun IdleHint() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            "Enter a query above to search",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ResultsList(
    state: SearchState.Results,
    listState: LazyListState,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.items) { _, item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(item, modifier = Modifier.padding(16.dp))
            }
        }

        if (state.isNextPageLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        } else if (state.hasMore) {
            item {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("Load more")
                }
            }
        } else {
            item {
                Text(
                    "No more results",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
