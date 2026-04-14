package com.umain.mediumandroidintegration.search

import com.umain.revolver.RevolverEffect

/**
 * One-shot side effects emitted by [SearchViewModel].
 */
sealed class SearchEffect : RevolverEffect {

    /** Scroll the results list back to the top after a new search. */
    data object ScrollToTop : SearchEffect()
}
