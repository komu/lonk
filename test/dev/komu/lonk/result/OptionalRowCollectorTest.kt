package dev.komu.lonk.result

import dev.komu.lonk.UnexpectedResultException
import kotlin.test.*

internal class OptionalRowCollectorTest {

    @Test
    fun `no rows produces null`() {
        val collector = OptionalRowCollector<Int> { error("should not be called") }

        assertNull(collector.finish())
    }

    @Test
    fun `single row produces its mapped value`() {
        val collector = OptionalRowCollector { it.get<Int>(0) }

        assertTrue(collector.accumulate(MockResultRow(1)))

        assertEquals(1, collector.finish())
    }

    @Test
    fun `more than one row throws`() {
        val collector = OptionalRowCollector { it.get<Int>(0) }

        assertTrue(collector.accumulate(MockResultRow(1)))
        assertFailsWith<UnexpectedResultException> {
            collector.accumulate(MockResultRow(2))
        }
    }

    @Test
    fun `row limit hint allows one extra row to detect duplicates`() {
        val collector = OptionalRowCollector<Int> { error("should not be called") }

        assertEquals(2, collector.rowLimitHint)
    }
}
