package dev.komu.lonk

import kotlin.reflect.KClass

public sealed class ResultKind<R> {
    internal class NullablePrimitive<T : Any>(internal val clazz: KClass<T>) : ResultKind<T?>()
}
