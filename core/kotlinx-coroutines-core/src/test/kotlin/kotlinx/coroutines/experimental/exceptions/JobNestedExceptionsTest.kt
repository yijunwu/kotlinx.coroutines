package kotlinx.coroutines.experimental.exceptions

import kotlinx.coroutines.experimental.*
import org.junit.Test
import java.io.*
import kotlin.coroutines.experimental.*
import kotlin.test.*

class JobNestedExceptionsTest : TestBase() {

    @Test
    fun testExceptionUnwrapping() {
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job) {
                expect(2)
                launch(coroutineContext) {
                    launch(coroutineContext) {
                        launch(coroutineContext) {
                            throw IllegalStateException()
                        }
                    }
                }
            }

            expect(1)
            job.join()
            finish(3)
        }

        checkException<IllegalStateException>(exception)
        checkCycles(exception)
    }

    @Test
    fun testExceptionUnwrappingWithSuspensions() {
        val exception = runBlock {
            val job = Job()
            launch(coroutineContext, parent = job) {
                expect(2)
                launch(coroutineContext) {
                    launch(coroutineContext) {
                        launch(coroutineContext) {
                            launch(coroutineContext) {
                                throw IOException()
                            }
                            yield()
                        }
                        delay(Long.MAX_VALUE)
                    }
                    delay(Long.MAX_VALUE)
                }
                delay(Long.MAX_VALUE)
            }

            expect(1)
            job.join()
            finish(3)
        }

        // TODO here cycles of JCE are present
        assertTrue(exception is IOException)
    }

    @Test
    @Ignore // TODO this test fails because it produces two exceptions
    fun testNestedAtomicThrow() {
        val exception = runBlock {
            expect(1)
            val job = launch(coroutineContext.minusKey(Job), CoroutineStart.ATOMIC) {
                val me = coroutineContext[Job]
                println("I'm throwing AE $this with parent $me")
                expect(2)

                launch(coroutineContext, CoroutineStart.ATOMIC) {
                    val me = coroutineContext[Job]
                    println("I'm throwing IOE: $this with parent $me")
                    expect(3)
                    launch(coroutineContext, CoroutineStart.ATOMIC) {
                        val me = coroutineContext[Job]
                        println("I'm throwing NPE: $this with parent $me")
                        expect(4)
                       // throw NullPointerException()
                    }

                    throw IOException()
                }

                throw ArithmeticException()
            }

            job.join()
            finish(5)
        }

        exception.printStackTrace()
        checkCycles(exception)
    }

    @Test
    @Ignore
    fun testNestedAtomicThrow2() {
        val exception = runBlock {
            expect(1)
            val job = launch(coroutineContext.minusKey(Job), CoroutineStart.ATOMIC) {
                expect(2)

                launch(coroutineContext, CoroutineStart.ATOMIC) {
                    expect(3)
                    throw IOException()
                }

                throw ArithmeticException()
            }

            job.join()
            finish(4)
        }

        assertTrue(exception is ArithmeticException)
        exception.printStackTrace()
    }
}
