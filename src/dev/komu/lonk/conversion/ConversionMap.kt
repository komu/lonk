package dev.komu.lonk.conversion

import dev.komu.lonk.utils.TypeUtils
import dev.komu.lonk.utils.wrapPrimitive
import java.lang.reflect.Type
import kotlin.reflect.KClass

internal class ConversionMap {
    private val mappings = mutableMapOf<Type, MutableList<ConversionRegistration>>()

    fun <S : Any, T : Any> register(source: KClass<S>, target: KClass<T>, conversion: TypeConversion<S, T>) {
        mappings.computeIfAbsent(source.wrapPrimitive().java) { mutableListOf() }
            .add(ConversionRegistration(target, conversion))
    }

    fun <S : Any, T : Any> findConversion(source: KClass<S>, target: KClass<T>): TypeConversion<S, T>? {
        var cl: Type? = source.wrapPrimitive().java
        while (cl != null) {
            val conversion = findConversionsRegisteredFor(cl, target)
            if (conversion != null) return conversion.unsafeCast()
            cl = TypeUtils.genericSuperClass(cl)
        }

        for (cl in source.java.getGenericInterfaces()) {
            val conversion = findConversionsRegisteredFor(cl, target)
            if (conversion != null) return conversion.unsafeCast()
        }

        return null
    }

    private fun <T : Any> findConversionsRegisteredFor(source: Type, target: KClass<T>): TypeConversion<*, T>? =
        mappings[source]?.asReversed()?.find { it.isAssignable(target) }?.conversion?.unsafeCast<Any, T>()

    private data class ConversionRegistration(val target: KClass<*>, val conversion: TypeConversion<*, *>) {

        fun isAssignable(cl: KClass<*>) = TypeUtils.isAssignable(cl, target)
    }
}
