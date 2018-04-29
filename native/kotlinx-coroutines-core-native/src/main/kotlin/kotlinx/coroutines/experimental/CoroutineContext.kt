/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental

import kotlin.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*

internal var currentEventLoop: BlockingEventLoop? = null

private fun takeEventLoop(): BlockingEventLoop =
    currentEventLoop ?: error("There is no event loop. Use runBlocking { ... } to start one.")

internal object DefaultExecutor : CoroutineDispatcher(), Delay {
    override fun dispatch(context: CoroutineContext, block: Runnable) =
        takeEventLoop().dispatch(context, block)
    override fun scheduleResumeAfterDelay(time: Long, unit: TimeUnit, continuation: CancellableContinuation<Unit>) =
        takeEventLoop().scheduleResumeAfterDelay(time, unit, continuation)
    override fun invokeOnTimeout(time: Long, unit: TimeUnit, block: Runnable): DisposableHandle =
        takeEventLoop().invokeOnTimeout(time, unit, block)

    fun execute(task: Runnable) {
        error("Cannot execute task because event loop was shut down")
    }

    fun schedule(delayedTask: EventLoopBase.DelayedTask) {
        error("Cannot schedule task because event loop was shut down")
    }

    fun removeDelayedImpl(delayedTask: EventLoopBase.DelayedTask) {
        error("Cannot happen")
    }
}

/**
 * This is the default [CoroutineDispatcher] that is used by all standard builders like
 * [launch], [async], etc if no dispatcher nor any other [ContinuationInterceptor] is specified in their context.
 */
public actual val DefaultDispatcher: CoroutineDispatcher = DefaultExecutor

internal actual val DefaultDelay: Delay = DefaultExecutor

/**
 * Creates context for the new coroutine. It installs [DefaultDispatcher] when no other dispatcher nor
 * [ContinuationInterceptor] is specified, and adds optional support for debugging facilities (when turned on).
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun newCoroutineContext(context: CoroutineContext, parent: Job? = null): CoroutineContext {
    val wp = if (parent == null) context else context + parent
    return if (context !== DefaultDispatcher && context[ContinuationInterceptor] == null)
        wp + DefaultDispatcher else wp
}

// No debugging facilities on native
internal actual inline fun <T> withCoroutineContext(context: CoroutineContext, block: () -> T): T = block()
internal actual fun Continuation<*>.toDebugString(): String = toString()
internal actual val CoroutineContext.coroutineName: String? get() = null // not supported on native
