package dev.komu.lonk.result

import dev.komu.lonk.UnexpectedResultException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class UniqueRowCollectorTest {

    @Test
    fun `no rows throws`() {
        val collector = UniqueRowCollector<Int> { error("should not be called") }

        assertFailsWith<UnexpectedResultException> {
            collector.finish()
        }
    }

    @Test
    fun `single row produces its mapped value`() {
        val collector = UniqueRowCollector { it.get<Int>(0) }

        assertTrue(collector.accumulate(MockResultRow(1)))

        assertEquals(1, collector.finish())
    }

    @Test
    fun `more than one row throws`() {
        val collector = UniqueRowCollector { it.get<Int>(0) }

        assertTrue(collector.accumulate(MockResultRow(1)))
        assertFailsWith<UnexpectedResultException> {
            collector.accumulate(MockResultRow(2))
        }
    }

    @Test
    fun `row limit hint allows one extra row to detect duplicates`() {
        val collector = UniqueRowCollector<Int> { error("should not be called") }

        assertEquals(2, collector.rowLimitHint)
    }
}
