package dev.komu.lonk.result

import dev.komu.lonk.conversion.DefaultTypeConversionRegistry
import dev.komu.lonk.instantiation.InstantiatorProvider
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InstantiatorRowMapperTest {

    private val instantiatorProvider = InstantiatorProvider(DefaultTypeConversionRegistry())

    @Test
    fun `instantiating with simple constructor`() {
        val mapper = ListRowCollector(InstantiatorRowMapper(SingleConstructor::class, instantiatorProvider))

        assertTrue(mapper.accumulate(MockResultRow(1, "foo")))
        assertTrue(mapper.accumulate(MockResultRow(3, "bar")))
        val list = mapper.finish()
        assertEquals(2, list.size)

        assertEquals(1, list[0].num)
        assertEquals("foo", list[0].str)
        assertEquals(3, list[1].num)
        assertEquals("bar", list[1].str)
    }

    @Test
    fun `empty result set produces no results`() {
        val mapper = ListRowCollector(InstantiatorRowMapper(SingleConstructor::class, instantiatorProvider))

        assertEquals(emptyList(), mapper.finish())
    }

    @Test
    fun `correct constructor is picked based on types`() {
        val mapper = ListRowCollector(InstantiatorRowMapper(TwoConstructors::class, instantiatorProvider))

        assertTrue(mapper.accumulate(MockResultRow(1, "foo")))
        val list = mapper.finish()
        assertEquals(1, list.size)

        assertEquals(1, list[0].num)
        assertEquals("foo", list[0].str)
    }

    class SingleConstructor(val num: Int, val str: String)

    class TwoConstructors(val num: Int, val str: String) {

        @Suppress("UNREACHABLE_CODE", "unused", "UNUSED_PARAMETER")
        constructor(num: Int, flag: Boolean) : this(
            throw RuntimeException("unexpected call two wrong constructor"), ""
        )
    }

    private class MockResultRow(override val values: List<Any>) : ResultRow {

        constructor(vararg values: Any) : this(values.asList())

        override fun get(name: String) = error("no used")

        override val columnCount: Int
            get() = values.size

        override fun getColumnLabel(index: Int): String =
            "column $index"

        override fun getColumnClass(index: Int): KClass<*> =
            values[index]::class

        override fun get(index: Int) = values[index]
    }
}
