package com.umain.mediumandroidintegration.fake

import com.umain.mediumandroidintegration.search.SearchRepository
import kotlinx.coroutines.delay

/**
 * In-memory [SearchRepository] that generates deterministic dummy results.
 *
 * Returns [pageSize] results for pages 0-1, then an empty list to signal end-of-results.
 * Simulates a 700 ms network delay per page.
 */
class FakeSearchRepository : SearchRepository {

    override suspend fun fetchPage(query: String, page: Int, pageSize: Int): List<String> {
        delay(700L)
        if (page >= 2) return emptyList() // only two pages of results
        return List(pageSize) { idx ->
            val globalIdx = page * pageSize + idx + 1
            "\"$query\" result #$globalIdx"
        }
    }
}
