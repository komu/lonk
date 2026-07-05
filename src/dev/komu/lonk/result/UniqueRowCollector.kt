package dev.komu.lonk.result

import dev.komu.lonk.UnexpectedResultException

internal class UniqueRowCollector<T>(private val rowMapper: ResultRowMapper<T>) : ResultRowCollector<T> {
    private var result: T? = null
    private var got = false

    override fun accumulate(row: ResultRow): Boolean {
        if (got) throw UnexpectedResultException("Expected unique result but received more than one row")
        result = rowMapper(row)
        got = true

        return true
    }

    override val rowLimitHint: Int
        get() = 2 // Even after the first row, we want to see more rows to ensure that there is only one result

    @Suppress("UNCHECKED_CAST")
    override fun finish() =
        if (got) result as T else throw UnexpectedResultException("Expected unique result, but got no rows")
}
