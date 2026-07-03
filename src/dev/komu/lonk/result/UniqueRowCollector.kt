package dev.komu.lonk.result

import dev.komu.lonk.EmptyResultException
import dev.komu.lonk.NonUniqueResultException

internal class UniqueRowCollector<T>(private val rowMapper: ResultRowMapper<T>) : ResultRowCollector<T> {
    private var result: T? = null
    private var got = false

    override fun accumulate(row: ResultRow): Boolean {
        if (got) throw NonUniqueResultException()
        result = rowMapper(row)
        got = true

        // Even after the first row, we want to see more rows to ensure that there is only one result
        return true
    }

    override val rowLimitHint: Int
        get() = 2

    @Suppress("UNCHECKED_CAST")
    override fun finish() = if (got) result as T else throw EmptyResultException()
}
