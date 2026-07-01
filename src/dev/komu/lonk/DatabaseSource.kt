package dev.komu.lonk

import dev.komu.lonk.adapter.ConnectionAdapter
import dev.komu.lonk.adapter.jdbc.JdbcConnectionAdapter
import dev.komu.lonk.conversion.DefaultTypeConversionRegistry
import dev.komu.lonk.conversion.TypeConversionRegistry
import dev.komu.lonk.instantiation.InstantiatorProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * A factory for [DatabaseConnection] instances, sharing a single configuration
 * (dialect and type conversions) across all connections it creates.
 * 
 * ```
 * val db = DatabaseSource(dataSource) {
 *     dispatcher = Dispathers.IO
 *     typeConversions.registerEnum(MyEnum::name)
 * }
 * db.withConnection { conn ->
 *     conn.findAll(MyType::class, "select ...");
 * }
 * ```
 * 
 * @see DatabaseConnection
 */
public abstract class DatabaseSource internal constructor() {

    public suspend inline fun <T> withConnection(callback: suspend (DatabaseConnection) -> T): T {
        val connection = openConnection()
        try {
            return callback(connection)
        } finally {
            connection.close()
        }
    }

    /**
     * Opens a new connection from the underlying data source with auto-commit disabled.
     * The caller is responsible for closing the connection, typically via try-with-resources.
     * Closing commits the transaction; see [DatabaseConnection.close] for details.
     * 
     * @throws DatabaseException if a connection cannot be obtained or configured
     */
    public abstract suspend fun openConnection(): DatabaseConnection

    public class Default<C> internal constructor(
        private val connectionAdapter: ConnectionAdapter<C>,
        private val instantiatorProvider: InstantiatorProvider,
    ) : DatabaseSource() {

        override suspend fun openConnection(): DefaultDatabaseConnection<C> = try {
            val connection = connectionAdapter.openConnection()

            DefaultDatabaseConnection(connection, connectionAdapter, instantiatorProvider)
        } catch (e: SQLException) {
            throw DatabaseSQLException(e)
        }
    }

    public companion object {

        public operator fun invoke(
            dataSource: DataSource,
            configurer: ConfigurationContext.() -> Unit = {}
        ): Default<Connection> {
            val typeConversionRegistry = DefaultTypeConversionRegistry()
            val config = ConfigurationContext(typeConversionRegistry)

            configurer(config)

            return Default(
                connectionAdapter = JdbcConnectionAdapter(dataSource, config.dispatcher),
                instantiatorProvider = InstantiatorProvider(typeConversionRegistry),
            )
        }

        public class ConfigurationContext(
            public val typeConversions: TypeConversionRegistry
        ) {
            public var dispatcher: CoroutineDispatcher = Dispatchers.IO
        }
    }
}
