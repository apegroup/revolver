package com.umain.revolver

import kotlinx.coroutines.CoroutineScope
import kotlin.native.concurrent.ThreadLocal

/**
 * In default implementation create main-thread dispatcher scope.
 */
@ThreadLocal
internal var createViewModelScope: () -> CoroutineScope = {
    CoroutineScope(createUIDispatcher())
}
