package com.umain.revolver.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

actual open class CSharedFlow<out T : Any> actual constructor(
    private val flow: SharedFlow<T>,
    private val coroutineScope: CoroutineScope,
    ) : SharedFlow<T> by flow
