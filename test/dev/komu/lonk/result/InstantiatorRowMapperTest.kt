package dev.komu.lonk.result

import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class InstantiatorRowMapperTest {

    private val instantiatorProvider = InstantiatorProvider(TypeConversionsConfigurer().build())

    @Test
    fun `instantiating with simple constructor`() {
        val mapper = InstantiatorRowMapper(SingleConstructor::class, instantiatorProvider)

        assertEquals(SingleConstructor(1, "foo"), mapper(MockResultRow(1, "foo")))
        assertEquals(SingleConstructor(3, "bar"), mapper(MockResultRow(3, "bar")))
    }

    @Test
    fun `correct constructor is picked based on types`() {
        val mapper = InstantiatorRowMapper(TwoConstructors::class, instantiatorProvider)

        assertEquals(TwoConstructors(1, "foo"), mapper(MockResultRow(1, "foo")))
    }

    data class SingleConstructor(val num: Int, val str: String)

    data class TwoConstructors(val num: Int, val str: String) {

        @Suppress("UNREACHABLE_CODE", "unused", "UNUSED_PARAMETER")
        constructor(num: Int, flag: Boolean) : this(
            error("unexpected call two wrong constructor"), ""
        )
    }
}
