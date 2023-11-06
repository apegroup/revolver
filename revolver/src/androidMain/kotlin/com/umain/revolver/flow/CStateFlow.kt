package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

actual open class CStateFlow<out T : Any> actual constructor(
    private val flow: StateFlow<T>,
    private val coroutineScope: CoroutineScope,
    ) : StateFlow<T> by flow
