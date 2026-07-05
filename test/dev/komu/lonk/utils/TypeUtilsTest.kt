package dev.komu.lonk.utils

import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TypeUtilsTest {

    @Test
    fun `raw type of generic type`() {
        val genericField = ExampleClass::class.members.first { it.name == "stringList" }
        assertEquals(List::class, genericField.returnType.rawType)
        assertEquals(List::class, genericField.returnType.rawType)
    }

    @Test
    fun `raw type of simple type`() {
        val genericField = ExampleClass::class.memberProperties.first { it.name == "string" }
        assertEquals(String::class, genericField.returnType.rawType)
    }

    class ExampleClass {
        var stringList: List<String> = emptyList()
        var stringArray: Array<String> = emptyArray()
        var string: String = ""
    }
}
