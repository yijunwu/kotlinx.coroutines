package kotlinx.coroutines.experimental

/**
 * Proxy for [Throwable.getSuppressed] for tests, which are compiled for both JDK 1.6 and JDK 1.8,
 * but run only under JDK 1.8
 */
fun Throwable.suppressed(): Array<Throwable> {
    val method = this::class.java.getMethod("getSuppressed") ?: error("This test can only be run using JDK 1.7")
    @Suppress("UNCHECKED_CAST")
    return method.invoke(this) as Array<Throwable>
}