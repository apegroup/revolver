package com.umain.revolver.flow

import kotlinx.coroutines.flow.StateFlow

actual open class CStateFlow<out T : Any> actual constructor(
    private val flow: StateFlow<T>,
) : CSharedFlow<T>(flow), StateFlow<T> {
    override val value: T get() = flow.value
}
