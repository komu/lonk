package dev.komu.lonk.conversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TypeConversionsConfigurerTest {

    private val configurer = TypeConversionsConfigurer()

    @Test
    fun `registerConversionFromDb and registerConversionToDb register independently`() {
        configurer.registerConversionFromDb<Int, String> { it.toString() }
        configurer.registerConversionToDb<String, Int> { it.length }

        val registry = configurer.build()

        assertEquals("42", registry.findConversionFromDbValue(Int::class, String::class)?.invoke(42))
        assertNull(registry.findConversionToDb(Int::class))

        val toDb = registry.findConversionToDb(String::class)
        assertEquals(3, toDb?.invoke("foo"))
    }

    @Test
    fun `registerConversions registers both directions`() {
        configurer.registerConversions<Int, String>({ it.toString() }, { it.toInt() })

        val registry = configurer.build()

        assertEquals("42", registry.findConversionFromDbValue(Int::class, String::class)?.invoke(42))

        val toDb = registry.findConversionToDb(String::class)
        assertEquals(42, toDb?.invoke("42"))
    }

    @Test
    fun `registerEnum converts to and from the key`() {
        configurer.registerEnum<Color, String> { it.code }

        val registry = configurer.build()

        assertEquals(Color.RED, registry.findConversionFromDbValue(String::class, Color::class)?.invoke("r"))

        val toDb = registry.findConversionToDb(Color::class)
        assertEquals("b", toDb?.invoke(Color.BLUE))
    }

    @Test
    fun `register delegates to the given TypeConversions`() {
        var registeredOn: TypeConversionsConfigurer? = null
        val conversions = object : TypeConversions {
            override fun registerOn(registry: TypeConversionsConfigurer) {
                registeredOn = registry
            }
        }

        configurer.register(conversions)

        assertEquals(configurer, registeredOn)
    }

    enum class Color(val code: String) {
        RED("r"), BLUE("b")
    }
}
