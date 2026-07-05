package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

/**
 * A [DbConnectionProvider] backed by a JDBC [DataSource].
 */
public class JdbcConnectionProvider internal constructor(
    private val dataSource: DataSource,
    config: Configuration,
) : DbConnectionProvider() {

    private val typeConversionRegistry = config.typeConversions.build()
    private val dispatcher = config.dispatcher
    private val instantiatorProvider = InstantiatorProvider(typeConversionRegistry)

    override suspend fun openConnection(autoCommit: Boolean): JdbcConnection = withContext(dispatcher) {
        val connection = dataSource.connection
        try {
            connection.autoCommit = autoCommit
            JdbcConnection(
                connection = connection,
                instantiatorProvider = instantiatorProvider,
                dispatcher = dispatcher
            )
        } catch (e: Throwable) {
            connection.closeSuppressing(e)
            throw e
        }
    }

    public companion object {
        /** Creates a [JdbcConnectionProvider] for [dataSource], optionally customized with [configurer]. */
        public operator fun invoke(
            dataSource: DataSource,
            configurer: Configuration.() -> Unit = {}
        ): JdbcConnectionProvider {
            val config = Configuration()

            configurer(config)

            return JdbcConnectionProvider(dataSource, config)
        }
    }

    /** Configuration options for a [JdbcConnectionProvider]. */
    public class Configuration internal constructor() {

        internal val typeConversions = TypeConversionsConfigurer()

        /**
         * CoroutineDispatcher to use for dispatching the database calls.
         *
         * By default [Dispatchers.IO], but it's a good idea to provide a custom pool that matches
         * the size of the connection pool.
         */
        public var dispatcher: CoroutineDispatcher = Dispatchers.IO

        /** Callback for registering custom type conversions. */
        public fun conversions(block: TypeConversionsConfigurer.() -> Unit) {
            block(typeConversions)
        }
    }
}

private fun Connection.closeSuppressing(e: Throwable) {
    try {
        close()
    } catch (e2: Throwable) {
        e.addSuppressed(e2)
    }
}

