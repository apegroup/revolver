package com.umain.mediumandroidintegration.search

/**
 * Contract for paginated search data fetching.
 *
 * Inject a real implementation in production; use [FakeSearchRepository] in examples/tests.
 */
interface SearchRepository {

    /**
     * Fetch a single page of results for [query].
     *
     * @param query the search term.
     * @param page zero-based page index.
     * @param pageSize maximum number of items to return.
     * @return a list of result labels; an empty list signals the end of results.
     */
    suspend fun fetchPage(query: String, page: Int, pageSize: Int): List<String>
}
