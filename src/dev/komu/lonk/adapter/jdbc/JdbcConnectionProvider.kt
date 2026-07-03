package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.conversion.*
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

public class JdbcConnectionProvider internal constructor(
    private val dataSource: DataSource,
    config: Configuration,
    typeConversionRegistry: DefaultTypeConversionRegistry,
) : DbConnectionProvider() {

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
        public operator fun invoke(
            dataSource: DataSource,
            configurer: Configuration.() -> Unit = {}
        ): JdbcConnectionProvider {
            val typeConversionRegistry = DefaultTypeConversionRegistry()
            val config = Configuration(typeConversionRegistry)

            typeConversionRegistry.register(NumberConversions)
            typeConversionRegistry.register(JavaTimeConversions)
            typeConversionRegistry.register(LobConversions)

            configurer(config)

            return JdbcConnectionProvider(dataSource, config, typeConversionRegistry)
        }
    }

    public class Configuration(
        public val typeConversions: TypeConversionRegistry
    ) {
        /**
         * CoroutineDispatcher to use for dispatching the database calls.
         *
         * By default [Dispatchers.IO], but it's a good idea to provide a custom pool that matches
         * the size of the connection pool.
         */
        public var dispatcher: CoroutineDispatcher = Dispatchers.IO
    }
}

private fun Connection.closeSuppressing(e: Throwable) {
    try {
        close()
    } catch (e2: Throwable) {
        e.addSuppressed(e2)
    }
}

