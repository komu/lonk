package dev.komu.lonk.instantiation

import dev.komu.lonk.conversion.TypeConversion
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.cast

internal class CompanionFunctionInstantiator<T : Any>(
    private val cl: KClass<T>,
    private val companion: Any,
    private val instantiator: KFunction<*>,
    private val conversions: List<TypeConversion<*, *>>,
) : Instantiator<T> {

    override fun instantiate(arguments: List<*>): T {
        val args = Array(instantiator.parameters.size) { i ->
            if (i == 0)
                companion
            else
                conversions[i - 1].convertUnsafe(arguments[i - 1])
        }

        return cl.cast(instantiator.call(*args))
    }
}
