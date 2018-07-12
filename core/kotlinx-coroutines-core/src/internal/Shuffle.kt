/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental.internal

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun <T> MutableList<T>.shuffleImpl() = shuffle()
