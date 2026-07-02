package dev.komu.lonk.conversion

import kotlin.reflect.KClass

/**
 * Registry containing the type-conversions used when converting database values
 * to model values and vice versa.
 */
public interface TypeConversionRegistry {

    public fun register(conversions: TypeConversions) {
        conversions.register(this)
    }

    /**
     * Registers conversion from the given source database type to the given target model type.
     */
    public fun <S : Any, T : Any> registerConversionFromDatabase(
        source: KClass<S>,
        target: KClass<T>,
        conversion: (S) -> T
    )

    /**
     * Registers conversion from given source model type to database type.
     */
    public fun <T : Any> registerConversionToDatabase(source: KClass<T>, conversion: (T) -> Any)

    /**
     * Registers conversions from database type to model type and back.
     */
    public fun <D : Any, J : Any> registerConversions(
        databaseType: KClass<D>,
        javaType: KClass<J>,
        fromDatabase: (D) -> J,
        toDatabase: (J) -> D,
    ) {
        registerConversionFromDatabase(databaseType, javaType, fromDatabase)
        registerConversionToDatabase(javaType, toDatabase)
    }
    /**
     * Registers simple enum conversion that uses keyFunction to produce saved value and uses
     * the same function on enum constants to convert values back.
     */
    public fun <T : Enum<T>, K : Any> registerEnum(enumType: KClass<T>, keyType: KClass<K>, keyFunction: (T) -> K)
}

public inline fun <reified D : Any, reified J : Any> TypeConversionRegistry.registerConversions(
    noinline fromDatabase: (D) -> J,
    noinline toDatabase: (J) -> D,
) {
    registerConversions(D::class, J::class, fromDatabase, toDatabase)
}

public inline fun <reified S : Any, reified T : Any> TypeConversionRegistry.registerConversionFromDatabase(noinline conversion: (S) -> T) {
    registerConversionFromDatabase(S::class, T::class, conversion)
}

public inline fun <reified T : Any> TypeConversionRegistry.registerConversionToDatabase(noinline conversion: (T) -> Any) {
    registerConversionToDatabase(T::class, conversion)
}

public inline fun <reified T : Enum<T>, reified K : Any> TypeConversionRegistry.registerEnum(noinline keyFunction: (T) -> K) {
    registerEnum(T::class, K::class, keyFunction)
}
