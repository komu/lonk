package dev.komu.lonk.utils

import dev.komu.lonk.InstantiationFailureException
import kotlin.test.*

internal class EnumProviderTest {

    private val provider = EnumProvider(Color::class)

    @Test
    fun `find by name`() {
        assertEquals(Color.RED, provider.findByName("RED"))
        assertEquals(Color.BLUE, provider.findByName("BLUE"))
    }

    @Test
    fun `find by name throws for unknown name`() {
        assertFailsWith<InstantiationFailureException> {
            provider.findByName("PURPLE")
        }
    }

    @Test
    fun `find by custom key`() {
        assertEquals(Color.RED, provider.findByKey("r") { it.code })
        assertEquals(Color.BLUE, provider.findByKey("b") { it.code })
    }

    @Test
    fun `find by custom key throws for unknown key`() {
        assertFailsWith<InstantiationFailureException> {
            provider.findByKey("z") { it.code }
        }
    }

    @Test
    fun `asEnumProviderOrNull returns provider for enum class`() {
        assertNotNull(Color::class.asEnumProviderOrNull())
    }

    @Test
    fun `asEnumProviderOrNull returns null for non-enum class`() {
        assertNull(String::class.asEnumProviderOrNull())
    }

    enum class Color(val code: String) {
        RED("r"), BLUE("b")
    }
}
