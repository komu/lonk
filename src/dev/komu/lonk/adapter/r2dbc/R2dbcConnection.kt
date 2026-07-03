package dev.komu.lonk.adapter.r2dbc

import dev.komu.lonk.DbConnection
import dev.komu.lonk.adapter.DatabaseQuery
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.ResultRowCollector
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.io.InputStream
import java.io.Reader

/**
 * A [DbConnection] backed by an R2DBC [Connection].
 */
public class R2dbcConnection internal constructor(

    /** The underlying R2DBC connection. */
    public val connection: Connection,
    instantiatorProvider: InstantiatorProvider,
    private val placeholderTranslation: PlaceholderTranslation,
) : DbConnection(instantiatorProvider) {

    override suspend fun update(query: DatabaseQuery): Long {
        val statement = connection.createStatement(query.translatedSql)
        statement.bindFrom(query)

        val result = statement.execute().awaitFirst()
        return result.rowsUpdated.awaitFirstOrNull() ?: 0
    }

    override suspend fun <T> executeQuery(query: DatabaseQuery, collector: ResultRowCollector<T>): T {
        val statement = connection.createStatement(query.translatedSql)

        val rowLimitHint = collector.rowLimitHint
        if (rowLimitHint != null)
            statement.fetchSize(rowLimitHint)

        statement.bindFrom(query)

        @OptIn(ExperimentalCoroutinesApi::class)
        statement.execute().asFlow()
            .flatMapConcat { result ->
                result.map { row, metadata -> collector.accumulate(R2DbcResultRow(row, metadata)) }
                    .asFlow()
            }
            .takeWhile { shouldContinue -> shouldContinue }
            .collect()

        return collector.finish()
    }


    override suspend fun close() {
        connection.close().awaitFirstOrNull()
    }

    override suspend fun commit() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override suspend fun rollback() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }

    private val DatabaseQuery.translatedSql: String
        get() = when (placeholderTranslation) {
            PlaceholderTranslation.PostgreSQL -> translatePlaceholdersForPostgreSQL(sql)
            PlaceholderTranslation.None -> sql
        }
}

private fun Statement.bindFrom(query: DatabaseQuery) {
    for ((i, value) in query.arguments.withIndex())
        bindArgument(i, value)
}

private fun Statement.bindArgument(index: Int, value: Any?) {
    when {
        value is InputStream -> bind(index, value.readAllBytes())
        value is Reader -> bind(index, value.readText())
        value != null -> bind(index, value)
        else -> bindNull(index, Any::class.java)
    }
}

