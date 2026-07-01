package dev.komu.lonk

import dev.komu.lonk.adapter.ConnectionAdapter
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.query.SqlQuery
import dev.komu.lonk.result.ResultSetProcessor
import java.sql.SQLException

public class DefaultDatabaseConnection<C> internal constructor(
    public val connection: C,
    private val connectionAdapter: ConnectionAdapter<C>,
    private val instantiatorRegistry: InstantiatorProvider,
) : DatabaseConnection(instantiatorRegistry) {

    /**
     * Executes a query and processes the results with given [dev.komu.lonk.result.ResultSetProcessor].
     * All other findXXX-methods are just convenience methods for this one.
     */
    override suspend fun <T> executeQuery(processor: ResultSetProcessor<T>, query: SqlQuery): T =
        connectionAdapter.executeQuery(connection, processor, query.toDatabase())

    @IgnorableReturnValue
    override suspend fun update(query: SqlQuery): Int =
        connectionAdapter.executeUpdate(connection, query.toDatabase())

    /**
     * Commits the pending transaction and closes the underlying JDBC connection.
     *
     * @throws DatabaseException if the commit, rollback, or close raises a SQL error
     */
    override suspend fun close() {
        try {
            try {
                if (this.isRollbackOnly) this.connectionAdapter.rollback(connection) else this.connectionAdapter.commit(
                    connection
                )
            } catch (e: SQLException) {
                try {
                    this.connectionAdapter.close(connection)
                } catch (e2: SQLException) {
                    e.addSuppressed(e2)
                }
                throw e
            }
            this.connectionAdapter.close(connection)
        } catch (e: SQLException) {
            throw DatabaseSQLException(e)
        }
    }

    private fun SqlQuery.toDatabase() = copy(
        arguments = arguments.map { instantiatorRegistry.valueToDatabase(it) },
        timeout = timeout ?: defaultTimeout
    )
}
