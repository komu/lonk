package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import kotlin.reflect.KClass
import kotlin.reflect.cast

internal class ImmediateSingleValueInstantiator<T : Any>(
    private val cl: KClass<T>,
    private val converter: TypeConversion = TypeConversion.identity,
) : Instantiator<T> {

    override fun instantiate(arguments: List<*>): T {
        require(arguments.size == 1) { "expected 1 argument, got ${arguments.size}" }
        return cl.cast(converter(arguments[0]))
    }
}
