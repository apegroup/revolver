package com.umain.mediumandroidintegration.search

import com.umain.revolver.Emitter
import com.umain.revolver.RevolverViewModel

/**
 * Manages paginated search with incremental list accumulation.
 *
 * Demonstrates:
 * - Reading [state].value inside a handler to derive the next state
 * - Partial state updates via `data class.copy()` to avoid full recomposition
 * - Guard clauses that make concurrent/duplicate events safe (LoadMore idempotency)
 * - Pagination: zero-based page index inferred from accumulated item count
 *
 * @param repository provides paginated search results.
 */
class SearchViewModel(
    private val repository: SearchRepository,
) : RevolverViewModel<SearchEvent, SearchState, SearchEffect>(
    initialState = SearchState.Idle,
) {

    init {
        addEventHandler<SearchEvent.Search>(::onSearch)
        addEventHandler<SearchEvent.LoadMore>(::onLoadMore)
    }

    private suspend fun onSearch(event: SearchEvent.Search, emit: Emitter<SearchState, SearchEffect>) {
        if (event.query.isEmpty()) {
            emit.state(SearchState.Idle)
            return
        }

        emit.state(SearchState.InitialLoading)

        try {
            val results = repository.fetchPage(event.query, page = 0, pageSize = PAGE_SIZE)
            emit.state(SearchState.Results(event.query, results, hasMore = results.size >= PAGE_SIZE))
            emit.effect(SearchEffect.ScrollToTop)
        } catch (e: Exception) {
            emit.state(SearchState.Error(e.message ?: "Failed to fetch results."))
        }
    }

    private suspend fun onLoadMore(
        event: SearchEvent.LoadMore,
        emit: Emitter<SearchState, SearchEffect>,
    ) {
        val current = state.value as? SearchState.Results ?: return
        if (current.isNextPageLoading || !current.hasMore) return

        emit.state(current.copy(isNextPageLoading = true))

        try {
            val nextPage = current.items.size / PAGE_SIZE
            val more = repository.fetchPage(current.query, page = nextPage, pageSize = PAGE_SIZE)
            emit.state(
                current.copy(
                    items = current.items + more,
                    isNextPageLoading = false,
                    hasMore = more.size >= PAGE_SIZE,
                ),
            )
        } catch (e: Exception) {
            // Restore the non-loading state so the user can retry.
            emit.state(current.copy(isNextPageLoading = false))
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
