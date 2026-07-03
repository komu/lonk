package dev.komu.lonk.result

import dev.komu.lonk.LonkException
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

internal class SingleNullableColumnInstantiatorRowMapper<T : Any>(
    private val cl: KClass<T>,
    private val instantiatorProvider: InstantiatorProvider
) : ResultRowMapper<T?> {

    private var instantiator: Instantiator<T>? = null

    override fun invoke(row: ResultRow): T? {
        if (row.columnCount != 1)
            throw LonkException("Expected exactly one column, got ${row.columnCount}")

        if (instantiator == null)
            instantiator = instantiatorProvider.findInstantiator(cl, row.types)

        return row[0]?.let { instantiator!!.instantiate(listOf(it)) }
    }
}
