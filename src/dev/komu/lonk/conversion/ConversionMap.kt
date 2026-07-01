package dev.komu.lonk.conversion

import dev.komu.lonk.utils.TypeUtils
import dev.komu.lonk.utils.wrapPrimitive
import java.lang.reflect.Type
import kotlin.reflect.KClass

internal class ConversionMap {
    private val mappings = mutableMapOf<Type, MutableList<ConversionRegistration>>()

    fun register(source: KClass<*>, target: KClass<*>, conversion: TypeConversion) {
        mappings.computeIfAbsent(source.wrapPrimitive().java) { mutableListOf() }
            .add(ConversionRegistration(target, conversion))
    }

    fun findConversion(source: KClass<*>, target: KClass<*>): TypeConversion? {
        var cl: Type? = source.wrapPrimitive().java
        while (cl != null) {
            val conversion = findConversionsRegisteredFor(cl, target)
            if (conversion != null) return conversion
            cl = TypeUtils.genericSuperClass(cl)
        }

        for (cl in source.java.getGenericInterfaces()) {
            val conversion = findConversionsRegisteredFor(cl, target)
            if (conversion != null) return conversion
        }

        return null
    }

    private fun findConversionsRegisteredFor(source: Type, target: KClass<*>): TypeConversion? =
        mappings[source]?.asReversed()?.find { it.isAssignable(target) }?.conversion

    private data class ConversionRegistration(val target: KClass<*>, val conversion: TypeConversion) {
        fun isAssignable(cl: KClass<*>) = TypeUtils.isAssignable(cl, target)
    }
}
