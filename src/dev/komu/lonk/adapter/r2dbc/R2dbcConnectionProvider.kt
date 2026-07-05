package dev.komu.lonk.adapter.r2dbc

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.conversion.TypeConversionsConfigurer
import dev.komu.lonk.instantiation.InstantiatorProvider
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * A [DbConnectionProvider] backed by an R2DBC [ConnectionFactory].
 */
public class R2dbcConnectionProvider internal constructor(
    private val connectionFactory: ConnectionFactory,
    private val config: Configuration,
) : DbConnectionProvider() {

    private val typeConversionRegistry = config.typeConversions.build()
    private val instantiatorProvider = InstantiatorProvider(typeConversionRegistry)

    override suspend fun openConnection(autoCommit: Boolean): R2dbcConnection {
        val connection = connectionFactory.create().awaitFirst()
        try {
            connection.setAutoCommit(autoCommit).awaitFirstOrNull()
            return R2dbcConnection(
                connection = connection,
                instantiatorProvider = instantiatorProvider,
                placeholderTranslation = config.placeholderTranslation
            )
        } catch (e: Throwable) {
            connection.closeSuppressing(e)
            throw e
        }
    }

    public companion object {
        /** Creates an [R2dbcConnectionProvider] for [connectionFactory], optionally customized with [configurer]. */
        public operator fun invoke(
            connectionFactory: ConnectionFactory,
            configurer: Configuration.() -> Unit = {}
        ): R2dbcConnectionProvider {
            val config = Configuration()

            configurer(config)

            return R2dbcConnectionProvider(connectionFactory, config)
        }

        /** Configuration options for an [R2dbcConnectionProvider]. */
        public class Configuration internal constructor() {

            internal val typeConversions = TypeConversionsConfigurer()

            /**
             * Should we automatically translate JDBC-style `?` placeholders in queries into
             * dialect-specific placeholders, or leave the query as it is?
             */
            public var placeholderTranslation: PlaceholderTranslation = PlaceholderTranslation.None

            /** Callback for registering custom type conversions. */
            public fun conversions(block: TypeConversionsConfigurer.() -> Unit) {
                block(typeConversions)
            }
        }
    }
}

private suspend fun Connection.closeSuppressing(e: Throwable) {
    try {
        close().awaitFirstOrNull()
    } catch (e2: Throwable) {
        e.addSuppressed(e2)
    }
}

