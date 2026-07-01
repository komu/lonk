package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import kotlin.reflect.KFunction

internal class CompanionFunctionInstantiator<T : Any>(
    private val companion: Any,
    private val instantiator: KFunction<*>,
    private val conversions: List<TypeConversion>,
) : Instantiator<T> {

    private val parameterCount = instantiator.parameters.size

    override fun instantiate(arguments: InstantiatorArguments): T? {
        val argumentArray = Array(parameterCount) { i ->
            if (i == 0)
                companion
            else
                conversions[i - 1].convert(arguments.values[i - 1])
        }

        return instantiator.call(*argumentArray) as T?
    }
}
