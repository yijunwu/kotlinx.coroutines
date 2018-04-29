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

package kotlinx.coroutines.experimental.internal

internal actual fun <E> arraycopy(source: Array<E>, srcPos: Int, destination: Array<E?>, destinationStart: Int, length: Int) {
    var destinationIndex = destinationStart
    for (sourceIndex in srcPos until srcPos + length) {
        destination[destinationIndex++] = source[sourceIndex]
    }
}
