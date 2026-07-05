package dev.komu.lonk.instantiation

import dev.komu.lonk.DbInstantiator
import dev.komu.lonk.InstantiationFailureException
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.instantiation.test.InaccessibleClassRef
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.test.*

internal class InstantiatorProviderTest {

    private val instantiatorProvider = InstantiatorProvider(TypeConversionsConfigurer().build())

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

    @Test
    fun `find instantiator uses registered conversion for single argument`() {
        val provider = InstantiatorProvider(TypeConversionsConfigurer().apply {
            registerConversionFromDb<Int, TestValueType> { TestValueType(it) }
        }.build())

        val instantiator = provider.findInstantiator(TestValueType::class, listOf(Int::class))

        assertEquals(TestValueType(42), instantiator.instantiate(listOf(42)))
    }

    @Test
    fun `find instantiator converts string to enum by name`() {
        val instantiator = instantiatorProvider.findInstantiator(Color::class, listOf(String::class))

        assertEquals(Color.BLUE, instantiator.instantiate(listOf("BLUE")))
    }

    @Test
    fun `find instantiator throws for unknown enum constant`() {
        val instantiator = instantiatorProvider.findInstantiator(Color::class, listOf(String::class))

        assertFailsWith<InstantiationFailureException> {
            instantiator.instantiate(listOf("PURPLE"))
        }
    }

    @Test
    fun `valueToDatabase returns null for null`() {
        assertNull(instantiatorProvider.valueToDatabase(null))
    }

    @Test
    fun `valueToDatabase returns enum name for enum values without registered conversion`() {
        assertEquals("BLUE", instantiatorProvider.valueToDatabase(Color.BLUE))
    }

    @Test
    fun `valueToDatabase returns value unchanged when no conversion applies`() {
        assertEquals("foo", instantiatorProvider.valueToDatabase("foo"))
    }

    @Test
    fun `valueToDatabase applies registered conversion`() {
        val provider = InstantiatorProvider(TypeConversionsConfigurer().apply {
            registerConversionToDb<TestValueType, Int> { it.value }
        }.build())

        assertEquals(42, provider.valueToDatabase(TestValueType(42)))
    }

    data class TestValueType(val value: Int)

    enum class Color { RED, BLUE }

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

        @DbInstantiator
        constructor(wrongType: Int) {
            this.publicField = wrongType.toString()
        }
    }

    class TestClassWithStaticInstantiator(val value: String) {

        companion object {
            @DbInstantiator
            fun instantiator(value: String) =
                TestClassWithStaticInstantiator("instantiator called: $value")
        }
    }

    class TestClassWithMultipleExplicitConstructors {
        @DbInstantiator
        constructor()

        @DbInstantiator
        constructor(@Suppress("UNUSED_PARAMETER") foo: String)
    }

    private fun <T : Any, V : Any> instantiate(cl: KClass<T>, type: KClass<V>, value: V): T =
        instantiate(cl, listOf(type), value)

    private fun <T : Any> instantiate(cl: KClass<T>, types: List<KClass<*>>, vararg values: Any?): T {
        val instantiator = instantiatorProvider.findInstantiator(cl, types)
        return instantiator.instantiate(values.asList())
    }

    private fun assertAssignable(target: KClass<*>, source: KClass<*>) {
        assertTrue(source.isSubclassOf(target))
    }

    class InaccessibleConstructor private constructor(@Suppress("UNUSED_PARAMETER") x: Int)
}
