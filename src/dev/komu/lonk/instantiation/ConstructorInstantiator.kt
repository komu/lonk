package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import kotlin.reflect.KFunction

/**
 * An instantiator that uses constructor and setters or fields to instantiate an object.
 */
internal class ConstructorInstantiator<T : Any>(
    private val ctor: KFunction<*>,
    private val conversions: List<TypeConversion>,
) : Instantiator<T> {

    private val parameterCount = ctor.parameters.size

    override fun instantiate(arguments: InstantiatorArguments): T? {
        val argumentArray = Array(parameterCount) { i -> conversions[i].convert(arguments.values[i]) }

        return ctor.call(*argumentArray) as T?
    }
}

