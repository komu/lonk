package dev.komu.lonk.utils

import kotlin.reflect.KClass

/**
 * If the type is a primitive type, returns the corresponding wrapper type, or the type itself if it is not a primitive type.
 */
internal fun <T : Any> KClass<T>.wrapPrimitive(): KClass<T> =
    javaObjectType.kotlin

