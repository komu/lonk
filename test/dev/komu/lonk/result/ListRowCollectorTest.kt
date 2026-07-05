package dev.komu.lonk.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ListRowCollectorTest {

    @Test
    fun `empty result set produces no results`() {
        val collector = ListRowCollector<Int> { error("should not be called") }

        assertEquals(emptyList(), collector.finish())
    }

    @Test
    fun `single result set produces single result`() {
        val collector = ListRowCollector { it.get<Int>(0) * it.get<Int>(1) }

        assertTrue(collector.accumulate(MockResultRow(1, 1)))
        assertTrue(collector.accumulate(MockResultRow(4, 2)))
        assertTrue(collector.accumulate(MockResultRow(3, 7)))

        assertEquals(listOf(1, 8, 21), collector.finish())
    }
}
