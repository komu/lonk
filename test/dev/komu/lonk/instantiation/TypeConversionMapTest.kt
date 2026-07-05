package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import dev.komu.lonk.conversion.TypeConversionMap
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class TypeConversionMapTest {

    private val registry = TypeConversionMap()

    @Test
    fun `searching for not existing item returns null`() {
        assertNull(registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `search based on exact match`() {
        val conversion = dummyConversion<Int, String>()
        registry.register(conversion)

        assertSame(conversion, registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `search based on result covariance`() {
        val conversion = dummyConversion<Int, String>()
        registry.register(conversion)

        assertSameConversion(conversion, registry.findConversion(Int::class, Any::class))
    }

    @Test
    fun `search based on param contravariance`() {
        val conversion = dummyConversion<Number, String>()
        registry.register(conversion)

        assertSameConversion(conversion, registry.findConversion(Int::class, String::class))
    }

    @Test
    fun `primitives and wrappers are considered same`() {
        val conversion = dummyConversion<Int, Long>()
        registry.register(conversion)

        assertSame(conversion, registry.findConversion(Int::class, Long::class))
    }

    @Test
    fun `source contravariance on interfaces`() {
        val conversion = dummyConversion<CharSequence, Long>()
        registry.register(conversion)

        assertSameConversion(conversion, registry.findConversion(String::class, Long::class))
    }

    @Test
    fun `later additions override earlier ones`() {
        val conversion1 = dummyConversion<String, Long>()
        val conversion2 = dummyConversion<String, Long>()
        registry.register(conversion1)
        registry.register(conversion2)

        assertSame(conversion2, registry.findConversion(String::class, Long::class))
    }


    private fun assertSameConversion(expected: TypeConversion<*, *>, actual: TypeConversion<*, *>?) {
        assertSame(expected, actual)
    }

    private inline fun <reified S : Any, reified T : Any> dummyConversion(): TypeConversion<S, T> =
        TypeConversion(S::class, T::class) { throw UnsupportedOperationException() }
}
