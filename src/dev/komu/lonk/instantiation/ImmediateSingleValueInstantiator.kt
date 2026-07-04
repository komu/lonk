package dev.komu.lonk.instantiation

import dev.komu.lonk.LonkException
import dev.komu.lonk.conversion.TypeConversion

internal class ImmediateSingleValueInstantiator<T : Any>(
    private val converter: TypeConversion<Any, T>,
) : Instantiator<T> {

    override fun instantiate(arguments: List<*>): T {
        require(arguments.size == 1) { "expected 1 argument, got ${arguments.size}" }

        val value = arguments.single() ?: throw LonkException("expected non-null argument")
        return converter(value)
    }
}
