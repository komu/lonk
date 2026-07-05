package dev.komu.lonk.conversion

import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf

internal class TypeConversionMap {

    /** Registered mapping by the source class */
    private val mappings = mutableMapOf<KClass<*>, MutableList<TypeConversion<*, *>>>()

    /**
     * Registers a new conversion.
     */
    fun <S : Any, T : Any> register(conversion: TypeConversion<S, T>) {
        mappings.computeIfAbsent(conversion.source) { mutableListOf() }.add(conversion)
    }

    /**
     * Finds the best conversion for a source/target pair.
     *
     * Try the source class itself first, then progressively less specific superclasses/interfaces.
     * This assumes conversions aren't registered on multiple unrelated interfaces implemented by the
     * same class (e.g., don't register separately for both `Clob` and some other interface a JDBC driver's
     * `Clob` impl happens to satisfy) -- there's no well-defined "most specific" between those.
     */
    fun <S : Any, T : Any> findConversion(source: KClass<S>, target: KClass<T>): TypeConversion<S, T>? {

        val candidates = listOf(source) + source.allSuperclasses

        return candidates.firstNotNullOfOrNull { findConversionsRegisteredFor(it, target) }?.cast(source, target)
    }

    /**
     * Search registrations for this exact class in reverse, so a later register() call overrides
     * an earlier one for the same source.
     */
    private fun <S : Any, T : Any> findConversionsRegisteredFor(source: KClass<S>, target: KClass<T>) =
        mappings[source]?.findLast { it.target.isSubclassOf(target) }
}
