package com.umain.mediumandroidintegration.search

import com.umain.revolver.RevolverState

/**
 * Immutable UI state for the paginated search screen.
 */
sealed class SearchState : RevolverState {

    /** The search field is empty and no search has been submitted yet. */
    data object Idle : SearchState()

    /** First page is loading after a new query was submitted. */
    data object InitialLoading : SearchState()

    /**
     * At least one page of results is available.
     *
     * @param query the active query these results belong to.
     * @param items all items accumulated across pages so far.
     * @param isNextPageLoading true while the next page is being fetched.
     * @param hasMore false when the repository signals no further pages exist.
     */
    data class Results(
        val query: String,
        val items: List<String>,
        val isNextPageLoading: Boolean = false,
        val hasMore: Boolean = true,
    ) : SearchState()

    /**
     * The initial search failed.
     *
     * @param message a human-readable error description.
     */
    data class Error(val message: String) : SearchState()
}
