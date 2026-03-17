package com.umain.mediumandroidintegration.search

import com.umain.revolver.RevolverEvent

/**
 * Actions the user can send to [SearchViewModel].
 */
sealed class SearchEvent : RevolverEvent {

    /**
     * Execute a new search, resetting any previous results.
     *
     * @param query the search term entered by the user.
     */
    data class Search(val query: String) : SearchEvent()

    /** Fetch the next page of results for the current query. */
    data object LoadMore : SearchEvent()
}
