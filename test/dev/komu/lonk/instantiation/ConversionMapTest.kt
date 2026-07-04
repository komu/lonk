package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.ConversionMap
import dev.komu.lonk.conversion.TypeConversion
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class ConversionMapTest {

    private val registry = ConversionMap()

    @Test
    fun `searching for not existing item returns null`() {
        assertNull(registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `search based on exact match`() {
        val conversion = dummyConversion<Int, String>()
        registry.register(Int::class, String::class, conversion)

        assertSame(conversion, registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `search based on result covariance`() {
        val conversion = dummyConversion<Int, String>()
        registry.register(Int::class, String::class, conversion)

        assertSame(conversion, registry.findConversion(Int::class, Any::class))
    }

    @Test
    fun `search based on param contravariance`() {
        val conversion = dummyConversion<Number, String>()
        registry.register(Number::class, String::class, conversion)

        assertSame(conversion, registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `primitives and wrappers are considered same`() {
        val conversion = dummyConversion<Int, Long>()
        registry.register(Int::class, Long::class, conversion)

        assertSame(conversion, registry.findConversion(Int::class, Long::class))
    }

    @Test
    fun `source contravariance on interfaces`() {
        val conversion = dummyConversion<CharSequence, Long>()
        registry.register(CharSequence::class, Long::class, conversion)

        assertSame(conversion, registry.findConversion(String::class, Long::class))
    }

    @Test
    fun `later additions override earlier ones`() {
        val conversion1 = dummyConversion<String, Long>()
        val conversion2 = dummyConversion<String, Long>()
        registry.register(String::class, Long::class, conversion1)
        registry.register(String::class, Long::class, conversion2)

        assertSame(conversion2, registry.findConversion(String::class, Long::class))
    }

    private fun <S, T> dummyConversion() =
        TypeConversion<S, T> { throw UnsupportedOperationException() }
}
