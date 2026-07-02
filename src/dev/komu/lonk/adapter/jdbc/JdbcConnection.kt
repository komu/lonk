package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.DatabaseQuery
import dev.komu.lonk.DbConnection
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.ResultAggregator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.Reader
import java.sql.Connection
import java.sql.PreparedStatement

public class JdbcConnection internal constructor(
    public val connection: Connection,
    instantiatorProvider: InstantiatorProvider,
    private val dispatcher: CoroutineDispatcher,
) : DbConnection(instantiatorProvider) {

    override suspend fun <T> doExecuteQuery(processor: ResultAggregator<T>, query: DatabaseQuery): T =
        withContext(dispatcher) {
            connection.prepareStatement(query.sql).use { ps ->
                ps.bindFrom(query)

                ps.executeQuery().use { rs ->
                    while (rs.next())
                        processor.process(JdbcResultRow(rs))
                }

                processor.build()
            }
        }

    override suspend fun doUpdate(query: DatabaseQuery): Long =
        withContext(dispatcher) {
            connection.prepareStatement(query.sql).use { ps ->
                ps.bindFrom(query)
                ps.executeLargeUpdate()
            }
        }

    override suspend fun commit() {
        withContext(dispatcher) {
            connection.commit()
        }
    }

    override suspend fun rollback() {
        withContext(dispatcher) {
            connection.rollback()
        }
    }

    override suspend fun close() {
        withContext(dispatcher) {
            connection.close()
        }
    }
}

private fun PreparedStatement.bindFrom(query: DatabaseQuery) {
    query.timeout?.let { this.queryTimeout = it.inWholeSeconds.toInt() }

    for ((i, arg) in query.arguments.withIndex())
        bindArgument(i + 1, arg)
}

private fun PreparedStatement.bindArgument(index: Int, value: Any?) {
    when (value) {
        is InputStream -> setBinaryStream(index, value)
        is Reader -> setCharacterStream(index, value)
        else -> setObject(index, value)
    }
}
