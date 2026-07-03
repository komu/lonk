package dev.komu.lonk.utils

import dev.komu.lonk.InstantiationFailureException
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

internal fun <T : Enum<T>, K> enumByKey(enumType: KClass<T>, keyFunction: (T) -> K, key: K): T {
    for (enumConstant in enumType.java.getEnumConstants())
        if (key == keyFunction(enumConstant))
            return enumConstant

    throw InstantiationFailureException("could not find enum constant of type ${enumType.jvmName} for $key")
}
