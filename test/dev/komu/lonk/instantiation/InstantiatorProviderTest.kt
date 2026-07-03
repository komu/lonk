package dev.komu.lonk.instantiation

import dev.komu.lonk.InstantiationFailureException
import dev.komu.lonk.conversion.DefaultTypeConversionRegistry
import dev.komu.lonk.instantiation.test.InaccessibleClassRef
import dev.komu.lonk.utils.TypeUtils
import kotlin.reflect.KClass
import kotlin.test.*

internal class InstantiatorProviderTest {

    private val instantiatorProvider = InstantiatorProvider(DefaultTypeConversionRegistry())

    @Test
    fun `every class is assignable from itself`() {
        assertAssignable(Int::class, Int::class)
        assertAssignable(Int::class, Int::class)
        assertAssignable(Any::class, Any::class)
        assertAssignable(String::class, String::class)
    }

    @Test
    fun `primitives are assignable from wrappers`() {
        assertAssignable(Int::class.javaPrimitiveType!!.kotlin, Int::class.javaObjectType.kotlin)
        assertAssignable(Long::class.javaPrimitiveType!!.kotlin, Long::class.javaObjectType.kotlin)
    }

    @Test
    fun `wrappers are assignable from primitives`() {
        assertAssignable(Int::class.javaObjectType.kotlin, Int::class.javaPrimitiveType!!.kotlin)
        assertAssignable(Long::class.javaObjectType.kotlin, Long::class.javaPrimitiveType!!.kotlin)
    }

    @Test
    fun `find default constructor`() {
        val result = assertNotNull(instantiate(TestClass::class, emptyList()))
        assertEquals(1, result.calledConstructor)
    }

    @Test
    fun `find constructed based on type`() {
        val result = assertNotNull(instantiate(TestClass::class, String::class, "foo"))
        assertEquals(2, result.calledConstructor)
    }

    @Test
    fun `find based on primitive type`() {
        val result = assertNotNull(instantiate(TestClass::class, Int::class, 3))
        assertEquals(3, result.calledConstructor)
    }

    @Test
    fun `find primitive typed constructor with boxed type`() {
        val result = assertNotNull(instantiate(TestClass::class, Int::class, 3))
        assertEquals(3, result.calledConstructor)
    }

    @Test
    fun `finding instantiator for inaccessible class throws nice exception`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(InaccessibleClassRef.INACCESSIBLE_CLASS, Int::class, 3)
        }
    }

    @Test
    fun `finding instantiator for inaccessible constructor throws nice exception`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(InaccessibleConstructor::class, Int::class, 3)
        }
    }

    @Test
    fun `dont use ignored constructor`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClass::class, listOf(Int::class, Int::class), 0, 0)
        }
    }

    @Test
    fun `explicit constructor is used instead of valid constructor`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClassWithExplicitConstructor::class, listOf(String::class), "foo")
        }
    }

    @Test
    fun `explicit constructor is used instead of valid property accessor`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClassWithExplicitConstructor::class, listOf(String::class), "bar")
        }
    }

    @Test
    fun `multiple constructor annotations gives nice error`() {
        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClassWithMultipleExplicitConstructors::class, Int::class, 1)
        }
    }

    @Test
    fun `static method as instantiator`() {
        val result = assertNotNull(instantiate(TestClassWithStaticInstantiator::class, listOf(String::class), "foo"))

        assertEquals("instantiator called: foo", result.value)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    class TestClass {
        val calledConstructor: Int

        var publicField = ""

        var propertyWithAccessors = ""

        constructor() {
            calledConstructor = 1
        }

        constructor(s: String) {
            calledConstructor = 2
        }

        constructor(x: Int) {
            calledConstructor = 3
        }
    }

    @Suppress("unused")
    class TestClassWithExplicitConstructor {
        var publicField = ""

        var propertyWithAccessors = ""

        constructor()

        constructor(publicField: String) {
            this.publicField = publicField
        }

        @LonkInstantiator
        constructor(wrongType: Int) {
            this.publicField = wrongType.toString()
        }
    }

    class TestClassWithStaticInstantiator(val value: String) {

        companion object {
            @LonkInstantiator
            fun instantiator(value: String) =
                TestClassWithStaticInstantiator("instantiator called: $value")
        }
    }

    class TestClassWithMultipleExplicitConstructors {
        @LonkInstantiator
        constructor()

        @LonkInstantiator
        constructor(@Suppress("UNUSED_PARAMETER") foo: String)
    }

    private fun <T : Any, V : Any> instantiate(cl: KClass<T>, type: KClass<V>, value: V): T =
        instantiate(cl, listOf(type), value)

    private fun <T : Any> instantiate(cl: KClass<T>, types: List<KClass<*>>, vararg values: Any?): T {
        val instantiator = instantiatorProvider.findInstantiator(cl, types)
        return instantiator.instantiate(values.asList())
    }

    private fun assertAssignable(target: KClass<*>, source: KClass<*>) {
        assertTrue(TypeUtils.isAssignable(target, source))
    }

    class InaccessibleConstructor private constructor(@Suppress("UNUSED_PARAMETER") x: Int)
}
