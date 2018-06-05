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

package kotlinx.coroutines.experimental.channels

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.internalAnnotations.*

/**
 * Rendezvous channel. This channel does not have any buffer at all. An element is transferred from sender
 * to receiver only when [send] and [receive] invocations meet in time (rendezvous), so [send] suspends
 * until another coroutine invokes [receive] and [receive] suspends until another coroutine invokes [send].
 *
 * Use `Channel()` factory function to conveniently create an instance of rendezvous channel.
 *
 * This implementation is fully lock-free.
 *
 * @param job Optional job that is bound to this channel's lifecycle. See [SendChannel.job].
 */
// Note: JvmOverloads ensures binary compatibility with job-less version of this constructor
public open class RendezvousChannel<E> @JvmOverloads public constructor(
    job: Job = ChannelJobImpl()
) : AbstractChannel<E>(job) {
    init {
        initChannelJob()
    }
    
    protected final override val isBufferAlwaysEmpty: Boolean get() = true
    protected final override val isBufferEmpty: Boolean get() = true
    protected final override val isBufferAlwaysFull: Boolean get() = true
    protected final override val isBufferFull: Boolean get() = true
}
