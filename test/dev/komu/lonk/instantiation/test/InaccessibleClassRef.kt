package dev.komu.lonk.instantiation.test

import kotlin.reflect.KClass

internal object InaccessibleClassRef {

    val INACCESSIBLE_CLASS: KClass<*> = InaccessibleClass::class
}

private class InaccessibleClass(@Suppress("UNUSED_PARAMETER") x: Int)
