package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import dev.komu.lonk.conversion.convertUnknownWith
import kotlin.reflect.KFunction

internal class ConstructorInstantiator<T : Any>(
    private val ctor: KFunction<T>,
    private val conversions: List<TypeConversion<*, *>>,
) : Instantiator<T> {

    init {
        require(ctor.parameters.size == conversions.size) { "ctor.parameters.size (${ctor.parameters.size}) != conversions.size (${conversions.size})" }
    }

    override fun instantiate(args: List<*>): T {
        val converted = Array(conversions.size) { i -> args[i]?.convertUnknownWith(conversions[i]) }
        return ctor.call(*converted)
    }
}
