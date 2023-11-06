package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

actual class CFlow<out T : Any> actual constructor(
    private val flow: Flow<T>,
    private val coroutineScope: CoroutineScope,
) : Flow<T> by flow
