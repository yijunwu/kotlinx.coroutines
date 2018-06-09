package kotlinx.coroutines.experimental.exceptions

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.CoroutineStart.*
import org.junit.*
import org.junit.Test
import java.io.*
import java.util.concurrent.*
import kotlin.coroutines.experimental.*
import kotlin.test.*

// TODO tests when cancel returns false
class JobExceptionHandlingTest : TestBase() {

    private val executor: ThreadPoolDispatcher by lazy { newFixedThreadPoolContext(8, "Exception handling tests") }

    @After
    fun tearDown() {
        executor.close()
    }

    @Test
    fun testExceptionDuringCancellation() {
        /*
         * Root parent: JobImpl()
         * Child: throws ISE
         * Launcher: cancels job
         * Result: ISE in exception handler
         *
         * Github issue #354
         */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job, start = ATOMIC) {
                expect(2)
                throw IllegalStateException()
            }

            expect(1)
            job.cancelAndJoin()
            finish(3)
        }

        checkException<IllegalStateException>(exception)
    }

    @Test
    fun testConsecutiveCancellation() {
        /*
         * Root parent: JobImpl()
         * Child: throws IOException
         * Launcher: cancels job with AE and then cancels with NPE
         * Result: IOE with suppressed AE with suppressed NPE
         */
        val exception = runBlock {
            val job = Job()
            val child = launch(coroutineContext, parent = job, start = ATOMIC) {
                expect(2)
                throw IOException()
            }

            expect(1)
            child.cancel(ArithmeticException())
            child.cancel(NullPointerException())
            job.join()
            finish(3)
        }

        assertTrue(exception is IOException)
        var suppressed = exception.suppressed()
        assertEquals(1, suppressed.size)
        val ae = suppressed[0] as ArithmeticException
        suppressed = ae.suppressed()
        assertEquals(1, suppressed.size)
        assertTrue(suppressed[0] is NullPointerException)
    }


    @Test
    fun testChildException() {
        /*
         * Root parent: JobImpl()
         * Child: throws ISE
         * Result: ISE in exception handler
         */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job, start = ATOMIC) {
                expect(2)
                throw IllegalStateException()
            }

            expect(1)
            job.join()
            finish(3)
        }

        checkException<IllegalStateException>(exception)
    }

    @Test
    fun testExceptionOnChildCancellation() {
        /*
         * Root parent: JobImpl()
         * Child: launch inner child and cancels parent
         * Inner child: throws AE
         * Result: AE in exception handler
         */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job) {
                expect(2) // <- child is launched successfully
                launch(coroutineContext) {
                    expect(3) // <- child's child is launched successfully
                    try {
                        yield()
                    } catch (e: JobCancellationException) {
                        throw ArithmeticException()
                    }
                }

                yield() // will throw cancellation exception
                expect(4)
                job.cancel()
            }

            expect(1)
            job.join()
            finish(5)
        }

        checkException<ArithmeticException>(exception)
    }

    @Test
    fun testExceptionOnChildCancellationWithCause() {
        /*
         * Root parent: JobImpl()
         * Child: launch inner child and cancels parent with IOE
         * Inner child: throws AE
         * Result: IOE with suppressed AE
         */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job) {
                expect(2) // <- child is launched successfully
                launch(coroutineContext) {
                    expect(3) // <- child's child is launched successfully
                    try {
                        yield()
                    } catch (e: JobCancellationException) {
                        throw ArithmeticException()
                    }
                }

                yield() // will throw cancellation exception
                expect(4)
                job.cancel(IOException())
            }

            expect(1)
            job.join()
            finish(5)
        }

        assertTrue(exception is IOException)
        assertNull(exception.cause)
        val suppressed = exception.suppressed()
        assertEquals(1, suppressed.size)
        checkException<ArithmeticException>(suppressed[0])
    }

    @Test
    fun testMultipleExceptionsOnChildCancellation() {
        /*
        * Root parent: JobImpl()
        * Owner: launch child and cancel root
        * Child: launch nested child atomically and yields
        * Inner child: throws AE
        * Result: AE
        */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job, start = ATOMIC) {
                expect(2)
                launch(coroutineContext, start = ATOMIC) {
                    expect(3) // <- child's child is launched successfully
                    throw ArithmeticException()
                }

                yield() // will throw cancellation exception
            }

            expect(1)
            job.cancel() // <- cancel is the first action
            job.join()
            finish(4)
        }

        checkException<ArithmeticException>(exception)
    }

    @Test
    fun testMultipleChildrenThrows() {
        /*
         * Root parent: launched job
         * Owner: launch 3 children, every of it throws an exception, and then call delay()
         * Result: one of the exceptions with the rest two as suppressed
         */
        val exception = runBlock(executor) {
            val barrier = CyclicBarrier(4)
            val job =
                launch(coroutineContext.minusKey(Job)) {
                    expect(2)
                    launch(coroutineContext) {
                        barrier.await()
                        throw ArithmeticException()
                    }

                    launch(coroutineContext) {
                        barrier.await()
                        throw IOException()
                    }

                    launch(coroutineContext) {
                        barrier.await()
                        throw IllegalArgumentException()
                    }

                    delay(Long.MAX_VALUE)
                }

            expect(1)
            barrier.await()
            job.join()
            finish(3)
        }


        val classes = mutableSetOf(
            IllegalArgumentException::class,
            IOException::class, ArithmeticException::class
        )

        assertTrue(classes.remove(exception::class), "Failed to remove ${exception::class} from $classes")
        for (throwable in exception.suppressed()) {
            assertTrue(classes.remove(throwable::class), "Failed to remove ${throwable::class} from $classes")
        }

        assertTrue(classes.isEmpty())
    }

    @Test
    fun testMultipleChildrenThrowsAtomically() {
        /*
          * Root parent: launched job
          * Owner: launch 3 children, every of it throws an exception, and then call delay()
          * Result: AE with suppressed IOE and IAE
          */
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job, start = ATOMIC) {
                expect(2)
                launch(
                    coroutineContext,
                    start = ATOMIC
                ) {
                    expect(3)
                    throw ArithmeticException()
                }

                launch(
                    coroutineContext,
                    start = ATOMIC
                ) {
                    expect(4)
                    throw IOException()
                }

                launch(
                    coroutineContext,
                    start = ATOMIC
                ) {
                    expect(5)
                    throw IllegalArgumentException()
                }

                delay(Long.MAX_VALUE)
            }

            expect(1)
            job.join()
            finish(6)
        }

        assertTrue(exception is ArithmeticException)
        val suppressed = exception.suppressed()
        assertEquals(2, suppressed.size)
        assertTrue(suppressed[0] is IOException)
        assertTrue(suppressed[1] is IllegalArgumentException)
    }
}
