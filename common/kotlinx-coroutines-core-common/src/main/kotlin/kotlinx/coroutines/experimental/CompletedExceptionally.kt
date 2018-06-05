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

import kotlinx.coroutines.experimental.internal.*
import kotlinx.coroutines.experimental.internalAnnotations.*
import kotlin.coroutines.experimental.*

/**
 * Class for an internal state of a job that had completed exceptionally, including cancellation.
 *
 * **Note: This class cannot be used outside of internal coroutines framework**.
 * **Note: cannot be internal until we get rid of MutableDelegateContinuation in IO**
 *
 * @param cause the exceptional completion cause. It's either original exceptional cause
 *        or artificial JobCancellationException if no cause was provided
 * @suppress **This is unstable API and it is subject to change.**
 */
open class CompletedExceptionally(
    @JvmField public val cause: Throwable
) {
    override fun toString(): String = "$classSimpleName[$cause]"
}

/**
 * A specific subclass of [CompletedExceptionally] for cancelled jobs.
 *
 * **Note: This class cannot be used outside of internal coroutines framework**.
 *
 * @param job the job that was cancelled.
 * @param cause the exceptional completion cause. If `cause` is null, then a [JobCancellationException] is created.
 * @suppress **This is unstable API and it is subject to change.**
 */
internal class CancelledJob(
    job: Job,
    cause: Throwable?
) : CompletedExceptionally(cause ?: JobCancellationException("Job was cancelled normally", null, job)) {

    // TODO optimize for null case with atomic addLast?
    private val throwables = LockFreeLinkedListHead()

    // TODO maybe merge with exception container
    // This field is owned by one thread and has HB edge on modification and reading TODO or not?
    public var isHandled = false

    // TODO lambda is always allocated
    public fun addLastIf(t: Throwable, condition: () -> Boolean): Boolean {
        return throwables.addLastIf(ExceptionNode(t), condition)
    }

    public fun mergeUpdates() = mergeUpdatesInto(cause)

    public fun mergeUpdatesInto(t: Throwable) {
        // Do not add JCE(cause) and cause to suppressed
        throwables.forEach<ExceptionNode> { node ->
            val exception = node.exception
            if (exception !== t && !(exception is CancellationException && exception.cause === t)) {
                t.addSuppressedThrowable(exception)
            }
        }
    }

    public fun dominatingException(): Throwable {
        val initial = unwrap(cause)
        if (initial !is CancellationException) {
            return initial
        }

        throwables.forEach<ExceptionNode> { node ->
            val result = unwrap(node.exception)
            if (result !is JobCancellationException) {
                return result
            }
        }

        return initial
    }

    private fun unwrap(exception: Throwable): Throwable {
        if (exception !is CancellationException) return exception
        var result = exception.cause ?: return exception
        while (result.cause != null) {
            result = result.cause ?: return result
        }

        return result
    }

    private class ExceptionNode(val exception: Throwable) : LockFreeLinkedListNode()
}

/**
 * A specific subclass of [CompletedExceptionally] for cancelled [AbstractContinuation].
 *
 * **Note: This class cannot be used outside of internal coroutines framework**.
 *
 * @param continuation the continuation that was cancelled.
 * @param cause the exceptional completion cause. If `cause` is null, then a [JobCancellationException]
 *        if created on first get from [exception] property.
 * @suppress **This is unstable API and it is subject to change.**
 */
public class CancelledContinuation(
    continuation: Continuation<*>,
    cause: Throwable?
) : CompletedExceptionally(cause ?: CancellationException("Continuation $continuation was cancelled normally"))
