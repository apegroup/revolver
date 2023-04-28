package com.umain.revolver

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun createUIDispatcher(): CoroutineDispatcher = Dispatchers.Main
