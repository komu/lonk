package dev.komu.lonk.result

import dev.komu.lonk.instantiation.Instantiator
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlin.reflect.KClass

internal class InstantiatorRowMapper<T : Any>(
    private val cl: KClass<T>,
    private val instantiatorProvider: InstantiatorProvider
) : ResultRowMapper<T> {

    private var instantiator: Instantiator<T>? = null

    override fun invoke(row: ResultRow): T {
        if (instantiator == null)
            instantiator = instantiatorProvider.findInstantiator(cl, row.types)

        return instantiator!!.instantiate(row.values)
    }
}
