package dev.komu.lonk

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * A factory for [DbConnection] instances, sharing a single configuration
 * (dialect and type conversions) across all connections it creates.
 *
 * @see dev.komu.lonk.adapter.jdbc.JdbcConnectionProvider
 * @see dev.komu.lonk.adapter.r2dbc.R2dbcConnectionProvider
 */
public abstract class DbConnectionProvider internal constructor() {

    /**
     * Execute a block of code in the context of a connection which is automatically closed
     * when the block completes or throws an exception.
     *
     * @see openConnection
     */
    public suspend inline fun <T> withTransaction(callback: suspend (DbConnection) -> T): T {
        val connection = openConnection(autoCommit = false)
        try {
            val result = callback(connection)
            connection.commit()
            return result
        } catch (e: Throwable) {
            withContext(NonCancellable) {
                try {
                    connection.rollback()
                } catch (e2: Throwable) {
                    e.addSuppressed(e2)
                }
            }
            throw e
        } finally {
            connection.close()
        }
    }

    /**
     * Execute a block of code in the context of a connection which is automatically closed
     * when the block completes or throws an exception.
     *
     * @see openConnection
     */
    public suspend inline fun <T> withConnection(autoCommit: Boolean, callback: suspend (DbConnection) -> T): T {
        val connection = openConnection(autoCommit = autoCommit)
        try {
            return callback(connection)
        } finally {
            connection.close()
        }
    }

    /**
     * Opens a new connection from the underlying data source.
     * The caller is responsible for closing the connection, see [DbConnection.close] for details.
     *
     * @see withTransaction
     * @see withConnection
     */
    public abstract suspend fun openConnection(autoCommit: Boolean): DbConnection
}
