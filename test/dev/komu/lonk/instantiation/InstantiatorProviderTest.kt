package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.DefaultTypeConversionRegistry
import dev.komu.lonk.instantiation.test.InaccessibleClassRef
import dev.komu.lonk.utils.TypeUtils
import kotlin.reflect.KClass
import kotlin.test.*

internal class InstantiatorProviderTest {

    private val instantiatorRegistry = InstantiatorProvider(DefaultTypeConversionRegistry())

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
        val result = assertNotNull(instantiate(TestClass::class, NamedTypeList.empty))
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
            instantiate(TestClass::class, createNamedTypeList(Int::class, Int::class), 0, 0)
        }
    }

    @Test
    fun `explicit constructor is used instead of valid constructor`() {
        val types = NamedTypeList.build("publicField" to String::class)

        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClassWithExplicitConstructor::class, types, "foo")
        }
    }

    @Test
    fun `explicit constructor is used instead of valid property accessor`() {
        val types = NamedTypeList.build("propertyWithAccessors" to String::class)

        assertFailsWith<InstantiationFailureException> {
            instantiate(TestClassWithExplicitConstructor::class, types, "bar")
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
        val types = NamedTypeList.build("foo" to String::class)
        val result = assertNotNull(instantiate(TestClassWithStaticInstantiator::class, types, "foo"))

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

    private fun <T : Any, V : Any> instantiate(cl: KClass<T>, type: KClass<V>, value: V): T? {
        return instantiate(cl, createNamedTypeList(type), value)
    }

    private fun <T : Any> instantiate(cl: KClass<T>, namedTypeList: NamedTypeList, vararg values: Any?): T? {
        val instantiator = instantiatorRegistry.findInstantiator(cl, namedTypeList)

        @Suppress("UNCHECKED_CAST")
        val arguments = InstantiatorArguments(namedTypeList, values as Array<Any>)
        return instantiator.instantiate(arguments)
    }

    private fun createNamedTypeList(vararg types: KClass<*>): NamedTypeList {
        val items = types.mapIndexed { i, cl -> "name$i" to cl }.toTypedArray()
        return NamedTypeList.build(*items)
    }

    private fun assertAssignable(target: KClass<*>, source: KClass<*>) {
        assertTrue(TypeUtils.isAssignable(target, source))
    }

    class InaccessibleConstructor private constructor(@Suppress("UNUSED_PARAMETER") x: Int)
}
