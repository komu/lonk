package dev.komu.lonk.result

import dev.komu.lonk.UnexpectedResultException

internal class OptionalRowCollector<T>(private val rowMapper: ResultRowMapper<T>) : ResultRowCollector<T?> {

    private var result: T? = null
    private var got = false

    override fun accumulate(row: ResultRow): Boolean {
        if (got) throw UnexpectedResultException("Expected at most one result but received more than one row")
        result = rowMapper(row)
        got = true

        // Even after the first row, we want to see more rows to ensure that there is only one result
        return true
    }

    override val rowLimitHint: Int
        get() = 2

    override fun finish() = result
}
