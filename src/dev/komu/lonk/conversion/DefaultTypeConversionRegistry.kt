package dev.komu.lonk.conversion

import dev.komu.lonk.utils.enumByKey
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * The used implementation of TypeConversionRegistry.
 */
internal class DefaultTypeConversionRegistry : TypeConversionRegistry {

    private val loadConversions = ConversionMap()
    private val storeConversions = ConversionMap()

    init {
        NumberConversions.register(this)
        JavaTimeConversions.register(this)
        LobConversions.register(this)
        // FIXME kotlinx-datetime?
    }

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

    fun findConversionFromDbValue(source: KClass<*>, target: KClass<*>): TypeConversion? =
        loadConversions.findConversion(source, target)

    fun findConversionToDb(type: KClass<*>): TypeConversion? =
        storeConversions.findConversion(type, Any::class)

    override fun <S : Any, T : Any> registerConversionFromDatabase(
        source: KClass<S>,
        target: KClass<T>,
        conversion: (S) -> T
    ) {
        loadConversions.register(source, target, TypeConversion.fromNonNullFunction { conversion(it as S) })
    }

    override fun <S : Any> registerConversionToDatabase(source: KClass<S>, conversion: (S) -> Any) {
        storeConversions.register(source, Any::class, TypeConversion.fromNonNullFunction { conversion(it as S) })
    }
}
