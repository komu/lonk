package dev.komu.lonk.instantiation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

internal class InstantiatorArgumentsTest {

    @Test
    fun `constructor arguments are retained`() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
        )

        val values = listOf("bar", 4)

        val arguments = InstantiatorArguments(types, values)
        assertSame(types, arguments.types)
        assertEquals(values, arguments.values)
    }

    @Test
    fun `sizes of argument lists differ`() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
        )

        assertFailsWith<IllegalArgumentException> {
            InstantiatorArguments(types, listOf("bar"))
        }
    }

    @Test
    fun `arguments size`() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
        )

        val arguments = InstantiatorArguments(types, listOf("bar", 4))
        assertEquals(2, arguments.size)
    }
}
