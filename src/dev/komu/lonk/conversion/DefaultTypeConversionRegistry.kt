package dev.komu.lonk.conversion

import dev.komu.lonk.utils.enumByKey
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * The used implementation of TypeConversionRegistry.
 */
internal class DefaultTypeConversionRegistry : ConversionsConfigurer {

    private val loadConversions = ConversionMap()
    private val storeConversions = ConversionMap()

    override fun <T : Enum<T>, K : Any> registerEnum(
        enumType: KClass<T>,
        keyType: KClass<K>,
        keyFunction: (T) -> K
    ) {
        registerConversionFromDatabase(Any::class, enumType) {
            enumByKey(enumType, keyFunction, keyType.cast(it))
        }
        registerConversionToDatabase(enumType, keyFunction)
    }

    fun <S : Any, T : Any> findConversionFromDbValue(source: KClass<S>, target: KClass<T>): TypeConversion<S, T>? =
        loadConversions.findConversion(source, target)

    fun <S : Any> findConversionToDb(type: KClass<S>): TypeConversion<S, *>? =
        storeConversions.findConversion(type, Any::class)

    override fun <S : Any, T : Any> registerConversionFromDatabase(
        source: KClass<S>,
        target: KClass<T>,
        conversion: (S) -> T
    ) {
        loadConversions.register(source, target, TypeConversion(conversion))
    }

    override fun <S : Any> registerConversionToDatabase(source: KClass<S>, conversion: (S) -> Any) {
        storeConversions.register(source, Any::class, TypeConversion(conversion))
    }
}
