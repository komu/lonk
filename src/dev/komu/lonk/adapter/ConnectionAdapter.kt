package dev.komu.lonk.adapter

import dev.komu.lonk.query.SqlQuery
import dev.komu.lonk.result.ResultSetProcessor

public interface ConnectionAdapter<C> {
    public suspend fun openConnection(): C

    public suspend fun executeUpdate(c: C, query: SqlQuery): Int
    public suspend fun <T> executeQuery(c: C, processor: ResultSetProcessor<T>, query: SqlQuery): T

    public suspend fun close(c: C)
    public suspend fun commit(c: C)
    public suspend fun rollback(c: C)

}
