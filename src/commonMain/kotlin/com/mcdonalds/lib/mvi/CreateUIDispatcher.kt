package com.mcdonalds.lib.mvi

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun createUIDispatcher(): CoroutineDispatcher = Dispatchers.Main
