package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.adapter.ConnectionAdapter
import dev.komu.lonk.query.SqlQuery
import dev.komu.lonk.result.ResultSetProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.sql.DataSource

internal class JdbcConnectionAdapter(
    private val dataSource: DataSource,
    private val dispatcher: CoroutineDispatcher,
) : ConnectionAdapter<Connection> {

    override suspend fun openConnection(): Connection = withContext(dispatcher) {
        val connection = dataSource.connection
        try {
            connection.autoCommit = false
        } catch (e: SQLException) {
            try {
                connection.close()
            } catch (e2: SQLException) {
                e.addSuppressed(e2)
            }
            throw e
        }
        connection
    }

    override suspend fun executeUpdate(c: Connection, query: SqlQuery): Int = withContext(dispatcher) {
        @Suppress("SqlSourceToSinkFlow")
        c.prepareStatement(query.sql).use { ps ->
            ps.bindFrom(query)
            ps.executeUpdate()
        }
    }

    override suspend fun <T> executeQuery(c: Connection, processor: ResultSetProcessor<T>, query: SqlQuery): T =
        withContext(dispatcher) {
            @Suppress("SqlSourceToSinkFlow")
            c.prepareStatement(query.sql).use { ps ->
                ps.bindFrom(query)

                ps.executeQuery().use { resultSet ->
                    processor.process(resultSet)
                }
            }
        }

    override suspend fun close(c: Connection) = withContext(dispatcher) {
        c.close()
    }

    override suspend fun commit(c: Connection) = withContext(dispatcher) {
        c.commit()
    }

    override suspend fun rollback(c: Connection) = withContext(dispatcher) {
        c.rollback()
    }
}

private fun PreparedStatement.bindFrom(query: SqlQuery) {
    query.fetchDirection?.let { this.fetchDirection = it.jdbcCode }
    query.fetchSize?.let { this.fetchSize = it }
    query.timeout?.let { this.queryTimeout = it.inWholeSeconds.toInt() }

    for ((i, arg) in query.arguments.withIndex())
        bindArgument(i + 1, arg)
}
