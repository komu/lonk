package dev.komu.lonk.utils

import kotlin.reflect.KClass
import kotlin.reflect.KType

internal val KType.rawType: KClass<*>
    get() = classifier as KClass<*>? ?: Any::class
