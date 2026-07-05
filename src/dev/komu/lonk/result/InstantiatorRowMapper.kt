package dev.komu.lonk.result

import dev.komu.lonk.instantiation.Instantiator
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlin.reflect.KClass

/**
 * Mapper for a given class.
 *
 * Note that this class is usable only for processing of rows of a single query because it
 * resolves the [Instantiator] only on the first invocation.
 */
internal class InstantiatorRowMapper<T : Any>(
    private val resultClass: KClass<T>,
    private val instantiatorProvider: InstantiatorProvider
) : ResultRowMapper<T> {

    private var instantiator: Instantiator<T>? = null

    override fun invoke(row: ResultRow): T {
        if (instantiator == null)
            instantiator = instantiatorProvider.findInstantiator(resultClass, row.types)

        return instantiator!!.instantiate(row.values)
    }
}
