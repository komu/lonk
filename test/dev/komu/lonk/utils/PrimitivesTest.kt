package dev.komu.lonk.utils

import kotlin.test.Test
import kotlin.test.assertEquals

internal class PrimitivesTest {

    @Test
    fun wrapping() {
        assertEquals(Char::class, Char::class.javaPrimitiveType!!.kotlin.wrapPrimitive())
        assertEquals(Boolean::class, Boolean::class.javaPrimitiveType!!.kotlin.wrapPrimitive())
        assertEquals(Boolean::class, Boolean::class.javaObjectType.kotlin.wrapPrimitive())
        assertEquals(String::class, String::class.java.kotlin.wrapPrimitive())
    }
}
