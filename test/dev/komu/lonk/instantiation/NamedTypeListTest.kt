package dev.komu.lonk.instantiation

import kotlin.test.Test
import kotlin.test.assertEquals

internal class NamedTypeListTest {

    @Test
    fun `readable toString`() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
            "baz" to Boolean::class,
        )

        assertEquals("[foo: kotlin.String, bar: kotlin.Int, baz: kotlin.Boolean]", types.toString())
    }

    @Test
    fun names() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
            "baz" to Boolean::class,
        )

        assertEquals("foo", types.getName(0))
        assertEquals("bar", types.getName(1))
        assertEquals("baz", types.getName(2))
    }

    @Test
    fun types() {
        val types = NamedTypeList.build(
            "foo" to String::class,
            "bar" to Int::class,
            "baz" to Boolean::class,
        )

        assertEquals(String::class, types.getType(0))
        assertEquals(Int::class, types.getType(1))
        assertEquals(Boolean::class, types.getType(2))
    }
}
