package dev.komu.lonk.conversion

import kotlin.reflect.KClass

/**
 * The used implementation of TypeConversionRegistry.
 */
internal class TypeConversionRegistry(
    private val loadConversions: TypeConversionMap,
    private val storeConversions: TypeConversionMap,
) {

    fun <S : Any, T : Any> findConversionFromDbValue(source: KClass<S>, target: KClass<T>): TypeConversion<S, T>? =
        loadConversions.findConversion(source, target)

    fun <S : Any> findConversionToDb(type: KClass<S>): TypeConversion<S, *>? =
        storeConversions.findConversion(type, Any::class)
}
