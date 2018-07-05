/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental.internal

import platform.posix.*

internal actual fun <T> MutableList<T>.shuffleImpl() {
    val n = size
    for (i in 0..n - 2) {
        val j = i + (random() % (n - i)).toInt()
        val t = get(i)
        set(i, get(j))
        set(j, t)
    }
}
