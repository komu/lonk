package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import kotlin.reflect.KFunction

internal class ConstructorInstantiator<T : Any>(
    private val ctor: KFunction<T>,
    private val conversions: List<TypeConversion<*, *>>,
) : Instantiator<T> {

    init {
        require(ctor.parameters.size == conversions.size) { "ctor.parameters.size (${ctor.parameters.size}) != conversions.size (${conversions.size})" }
    }

    override fun instantiate(arguments: List<*>): T {
        val args = Array(conversions.size) { i -> conversions[i].convertUnsafe(arguments[i]) }
        return ctor.call(*args)
    }
}
