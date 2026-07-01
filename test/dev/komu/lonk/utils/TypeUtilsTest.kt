package dev.komu.lonk.utils

import dev.komu.lonk.utils.TypeUtils.arrayType
import dev.komu.lonk.utils.TypeUtils.rawType
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TypeUtilsTest {

    @Test
    fun `raw type of generic type`() {
        val genericField = ExampleClass::class.java.getField("stringList")
        assertEquals(List::class, rawType(genericField.genericType))
        assertEquals(List::class, rawType(genericField.type))
    }

    @Test
    fun `raw type of array type`() {
        val genericField = ExampleClass::class.java.getField("stringArray")
        assertEquals(Array<String>::class, rawType(genericField.genericType))
        assertEquals(Array<String>::class, rawType(genericField.type))
    }

    @Test
    fun `raw type of simple type`() {
        val genericField = ExampleClass::class.java.getField("string")
        assertEquals(String::class, rawType(genericField.genericType))
        assertEquals(String::class, rawType(genericField.type))
    }

    @Test
    fun `array types`() {
        assertEquals(Array<String>::class, arrayType(String::class))
        assertEquals(Array<Int>::class, arrayType(Int::class.javaObjectType.kotlin))
        assertEquals(IntArray::class, arrayType(Int::class))
    }

    @Suppress("unused")
    class ExampleClass {
        @JvmField var stringList: List<String>? = null
        @JvmField var stringArray: Array<String>? = null
        @JvmField var string: String? = null
    }
}
