package dev.komu.lonk.conversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TypeConversionRegistryTest {

    private val loadConversions = TypeConversionMap()
    private val storeConversions = TypeConversionMap()
    private val registry = TypeConversionRegistry(loadConversions, storeConversions)

    @Test
    fun `findConversionFromDbValue delegates to load conversions`() {
        val conversion = TypeConversion(Int::class, String::class) { it.toString() }
        loadConversions.register(conversion)

        assertEquals("42", registry.findConversionFromDbValue(Int::class, String::class)?.invoke(42))
    }

    @Test
    fun `findConversionFromDbValue returns null when nothing registered`() {
        assertNull(registry.findConversionFromDbValue(Int::class, String::class))
    }

    @Test
    fun `findConversionToDb delegates to store conversions`() {
        val conversion = TypeConversion(String::class, Int::class) { it.length }
        storeConversions.register(conversion)

        val found = registry.findConversionToDb(String::class)

        assertEquals(3, found?.invoke("foo"))
    }

    @Test
    fun `findConversionToDb returns null when nothing registered`() {
        assertNull(registry.findConversionToDb(String::class))
    }
}
