package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.DbConnection
import dev.komu.lonk.adapter.DatabaseQuery
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.ResultRowCollector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.Reader
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLFeatureNotSupportedException
import java.sql.Statement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A [DbConnection] backed by a plain JDBC [Connection].
 */
public class JdbcConnection internal constructor(
    /** The underlying JDBC connection. */
    public val connection: Connection,
    instantiatorProvider: InstantiatorProvider,
    private val dispatcher: CoroutineDispatcher,
) : DbConnection(instantiatorProvider) {

    override suspend fun <T> executeQuery(query: DatabaseQuery, collector: ResultRowCollector<T>): T =
        withContext(dispatcher) {
            connection.prepareStatement(query.sql).useCancellable { ps ->
                val rowLimitHint = collector.rowLimitHint
                if (rowLimitHint != null)
                    ps.maxRows = rowLimitHint

                ps.bindFrom(query)

                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val cont = collector.accumulate(JdbcResultRow(rs))
                        if (!cont)
                            break
                    }
                }

                collector.finish()
            }
        }

    override suspend fun update(query: DatabaseQuery): Long =
        withContext(dispatcher) {
            connection.prepareStatement(query.sql).useCancellable { ps ->
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

private suspend fun <T, S : Statement> S.useCancellable(block: (S) -> T): T =
    use { stmt ->
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                try {
                    stmt.cancel()
                } catch (_: SQLFeatureNotSupportedException) {
                    // No cancel support on the driver. This is ok. Cancellations are best-effort.
                } catch (_: Exception) {
                    // Oops, something failed. However, we are not allowed to throw in this context. Ignore.
                }
            }
            try {
                cont.resume(block(stmt))
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }
    }
