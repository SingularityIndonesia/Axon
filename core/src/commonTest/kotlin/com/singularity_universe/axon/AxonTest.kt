package com.singularity_universe.axon

import com.singularity_universe.axon.exception.DuplicateResolverException
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class AxonTest {

    private data class TestIntent(val value: String) : Intent<String>()
    private data class OtherIntent(val value: Int) : Intent<Int>()

    private fun resolver(block: suspend (TestIntent) -> String): Resolver<TestIntent, String> =
        object : Resolver<TestIntent, String> {
            override suspend fun resolve(intent: TestIntent): String = block(intent)
        }

    // --- dispatch ---

    @Test
    fun `dispatch returns result from resolver`() = runTest {
        val axon = Axon()
        axon.registerResolver(TestIntent::class, lazy { resolver { "result:${it.value}" } })
        assertEquals("result:hello", axon.dispatch(TestIntent("hello")))
    }

    @Test
    fun `dispatch passes intent to resolver`() = runTest {
        val axon = Axon()
        var received: TestIntent? = null
        axon.registerResolver(TestIntent::class, lazy { resolver { received = it; "ok" } })
        val intent = TestIntent("ping")
        axon.dispatch(intent)
        assertEquals(intent, received)
    }

    @Test
    fun `dispatch throws NoHandlerException when no resolver registered`() = runTest {
        val axon = Axon()
        assertFailsWith<NoHandlerException> {
            axon.dispatch(TestIntent("hello"))
        }
    }

    @Test
    fun `dispatch throws ResolverException when resolver throws`() = runTest {
        val axon = Axon(logger = object : AxonLogger {
            override fun onResolverException(intent: Intent<*>, exception: Throwable) = Unit
        })
        axon.registerResolver(TestIntent::class, lazy { resolver { throw RuntimeException("oops") } })
        assertFailsWith<ResolverException> {
            axon.dispatch(TestIntent("hello"))
        }
    }

    @Test
    fun `dispatch rethrows CancellationException without wrapping`() = runTest {
        val axon = Axon()
        axon.registerResolver(TestIntent::class, lazy { resolver { throw CancellationException("cancelled") } })
        var caught: Throwable? = null
        try {
            axon.dispatch(TestIntent("hello"))
        } catch (e: CancellationException) {
            caught = e
        } catch (e: ResolverException) {
            fail("CancellationException must not be wrapped in ResolverException")
        }
        assertNotNull(caught)
        assertEquals("cancelled", caught.message)
    }

    @Test
    fun `dispatch works independently for different intent types`() = runTest {
        val axon = Axon()
        axon.registerResolver(TestIntent::class, lazy { resolver { "string:${it.value}" } })
        axon.registerResolver(OtherIntent::class, lazy {
            object : Resolver<OtherIntent, Int> {
                override suspend fun resolve(intent: OtherIntent): Int = intent.value * 2
            }
        })
        assertEquals("string:hello", axon.dispatch(TestIntent("hello")))
        assertEquals(42, axon.dispatch(OtherIntent(21)))
    }

    // --- registerResolver ---

    @Test
    fun `registerResolver throws DuplicateResolverException on duplicate`() {
        val axon = Axon()
        axon.registerResolver(TestIntent::class, lazy { resolver { "first" } })
        assertFailsWith<DuplicateResolverException> {
            axon.registerResolver(TestIntent::class, lazy { resolver { "second" } })
        }
    }

    // --- lazy instantiation ---

    @Test
    fun `resolver is not instantiated until first dispatch`() = runTest {
        val axon = Axon()
        var instantiated = false
        axon.registerResolver(TestIntent::class, lazy {
            instantiated = true
            resolver { "ok" }
        })
        assertTrue(!instantiated, "Resolver must not be instantiated at registration time")
        axon.dispatch(TestIntent("hello"))
        assertTrue(instantiated, "Resolver must be instantiated after first dispatch")
    }

    @Test
    fun `resolver instance is reused across multiple dispatches`() = runTest {
        val axon = Axon()
        var instanceCount = 0
        axon.registerResolver(TestIntent::class, lazy {
            instanceCount++
            resolver { "ok" }
        })
        axon.dispatch(TestIntent("a"))
        axon.dispatch(TestIntent("b"))
        axon.dispatch(TestIntent("c"))
        assertEquals(1, instanceCount, "Resolver must be instantiated exactly once")
    }

    // --- logger ---

    @Test
    fun `logger receives intent and exception when resolver throws`() = runTest {
        var loggedIntent: Intent<*>? = null
        var loggedException: Throwable? = null
        val axon = Axon(logger = object : AxonLogger {
            override fun onResolverException(intent: Intent<*>, exception: Throwable) {
                loggedIntent = intent
                loggedException = exception
            }
        })
        val intent = TestIntent("hello")
        axon.registerResolver(TestIntent::class, lazy { resolver { throw RuntimeException("boom") } })
        runCatching { axon.dispatch(intent) }
        assertEquals(intent, loggedIntent)
        assertEquals("boom", loggedException?.message)
    }

    @Test
    fun `logger is not called when resolver succeeds`() = runTest {
        var loggerCalled = false
        val axon = Axon(logger = object : AxonLogger {
            override fun onResolverException(intent: Intent<*>, exception: Throwable) {
                loggerCalled = true
            }
        })
        axon.registerResolver(TestIntent::class, lazy { resolver { "ok" } })
        axon.dispatch(TestIntent("hello"))
        assertTrue(!loggerCalled)
    }
}
