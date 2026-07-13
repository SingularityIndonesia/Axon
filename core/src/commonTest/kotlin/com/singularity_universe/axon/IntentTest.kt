package com.singularity_universe.axon

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntentTest {

    private data class SomeIntent(val value: String) : Intent<String>()

    @Test
    fun `createdAt is set at construction time`() {
        val before = Clock.System.now().toEpochMilliseconds()
        val intent = SomeIntent("test")
        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(intent.createdAt >= before, "createdAt must not be before construction")
        assertTrue(intent.createdAt <= after, "createdAt must not be after construction")
    }

    @Test
    fun `parent defaults to null`() {
        assertNull(SomeIntent("test").parent)
    }

    @Test
    fun `parent can be set at construction time`() {
        val parent = SomeIntent("parent")
        val child = object : Intent<String>(parent) {}
        assertEquals(parent, child.parent)
    }

    @Test
    fun `two intents created sequentially have non-decreasing createdAt`() {
        val first = SomeIntent("first")
        val second = SomeIntent("second")
        assertTrue(second.createdAt >= first.createdAt)
    }
}
