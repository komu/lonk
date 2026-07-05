package dev.komu.lonk.conversion

import dev.komu.lonk.utils.EnumProvider
import kotlin.reflect.KClass

/**
 * Registry containing the type-conversions used when converting database values
 * to model values and vice versa.
 */
public class TypeConversionsConfigurer internal constructor() {

    private val loadConversions = TypeConversionMap()
    private val storeConversions = TypeConversionMap()

    /** Registers all conversions contained in [conversions]. */
    public fun register(conversions: TypeConversions) {
        conversions.registerOn(this)
    }

    /**
     * Registers conversions from database type to model type and back.
     *
     * @see registerConversionFromDb
     * @see registerConversionToDb
     */
    public inline fun <reified D : Any, reified M : Any> registerConversions(
        noinline fromDb: (D) -> M,
        noinline toDb: (M) -> D,
    ) {
        registerConversionFromDb(D::class, M::class, fromDb)
        registerConversionToDb(M::class, D::class, toDb)
    }

    /**
     * Registers an enum conversion that uses the [key] function to produce a saved value
     * and the same function on enum constants to convert values back.
     *
     * The [key] must be injective and the resulting values must have equality defined.
     */
    public inline fun <reified T : Enum<T>, reified K : Any> registerEnum(noinline key: (T) -> K) {
        registerEnum(T::class, K::class, key)
    }

    /**
     * Registers an enum conversion that uses the [key] function to produce a saved value
     * and the same function on enum constants to convert values back.
     *
     * The [key] must be injective and the resulting values must have equality defined.
     */
    public fun <T : Enum<T>, K : Any> registerEnum(enumType: KClass<T>, keyType: KClass<K>, key: (T) -> K) {
        val enumProvider = EnumProvider(enumType)

        registerConversionFromDb(keyType, enumType) { enumProvider.findByKey(it, key) }
        registerConversionToDb(enumType, keyType, key)
    }


    /**
     * Registers conversion from the given source database type to the given target model type.
     */
    public inline fun <reified S : Any, reified T : Any> registerConversionFromDb(noinline conversion: (S) -> T) {
        registerConversionFromDb(S::class, T::class, conversion)
    }

    /**
     * Registers conversion from the given source database type to the given target model type.
     */
    public fun <S : Any, T : Any> registerConversionFromDb(
        databaseType: KClass<S>,
        modelType: KClass<T>,
        conversion: (S) -> T
    ) {
        loadConversions.register(TypeConversion(databaseType, modelType, conversion))
    }

    /**
     * Registers conversion from given source model type to database type.
     */
    public inline fun <reified M : Any, reified D : Any> registerConversionToDb(noinline conversion: (M) -> D) {
        registerConversionToDb(M::class, D::class, conversion)
    }

    /**
     * Registers conversion from the given source model type to the given database type.
     */
    public fun <M : Any, D : Any> registerConversionToDb(
        modelType: KClass<M>,
        databaseType: KClass<D>,
        conversion: (M) -> D
    ) {
        storeConversions.register(TypeConversion(modelType, databaseType, conversion))
    }

    internal fun build() = TypeConversionRegistry(loadConversions, storeConversions)
}
