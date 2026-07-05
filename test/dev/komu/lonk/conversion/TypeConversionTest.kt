package dev.komu.lonk.conversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

internal class TypeConversionTest {

    @Test
    fun `invoking applies the conversion`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        assertEquals("42", conversion(42))
    }

    @Test
    fun `identity conversion returns its argument unchanged`() {
        val conversion = TypeConversion.identity(String::class)

        assertEquals("foo", conversion("foo"))
    }

    @Test
    fun `cast widens source and target as long as classes are compatible`() {
        val conversion = TypeConversion(Number::class, String::class) { it.toString() }

        val cast = conversion.cast(Int::class, Any::class)

        assertEquals("42", cast(42))
    }

    @Test
    fun `cast fails if source is not a subclass of the conversion source`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        assertFailsWith<IllegalArgumentException> {
            conversion.cast(String::class, Any::class)
        }
    }

    @Test
    fun `cast fails if conversion target is not a subclass of target`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        assertFailsWith<IllegalArgumentException> {
            conversion.cast(Int::class, Int::class)
        }
    }

    @Test
    fun `convertUnknownWith casts the receiver and applies the conversion`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        val value: Any = 42
        assertEquals("42", value.convertUnknownWith(conversion))
    }

    @Test
    fun `convertUnknownWith fails if the receiver does not match the source type`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        val value: Any = "not an int"
        assertFailsWith<ClassCastException> {
            value.convertUnknownWith(conversion)
        }
    }

    @Test
    fun `source and target are exposed`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }

        assertSame(Int::class, conversion.source)
        assertSame(String::class, conversion.target)
    }
}
