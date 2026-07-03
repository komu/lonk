package dev.komu.lonk.result

internal class ListRowCollector<T>(private val rowMapper: ResultRowMapper<T>) : ResultRowCollector<List<T>> {
    private val result = mutableListOf<T>()

    override fun accumulate(row: ResultRow): Boolean {
        result += rowMapper(row)
        return true
    }

    override fun finish() =
        result
}
