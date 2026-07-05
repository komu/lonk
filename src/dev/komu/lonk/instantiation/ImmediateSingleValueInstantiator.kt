package dev.komu.lonk.instantiation

import dev.komu.lonk.InstantiationFailureException
import dev.komu.lonk.conversion.TypeConversion
import dev.komu.lonk.conversion.convertUnknownWith

internal class ImmediateSingleValueInstantiator<T : Any>(
    private val converter: TypeConversion<*, T>,
) : Instantiator<T> {

    override fun instantiate(args: List<*>): T {
        require(args.size == 1) { "expected 1 argument, got ${args.size}" }

        val value = args.single() ?: throw InstantiationFailureException("expected non-null result")
        return value.convertUnknownWith(converter)
    }
}
