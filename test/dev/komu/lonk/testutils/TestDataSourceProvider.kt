package dev.komu.lonk.testutils

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.junit.jupiter.api.Assumptions
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

internal enum class DatabaseProvider {
    POSTGRESQL,
    HSQL,
}

internal object TestDataSourceProvider {

    private val postgresqlContainer by lazy {
        PostgreSQLContainer("postgres:17").also { it.start() }
    }

    fun createDataSource(provider: DatabaseProvider): DataSource = when (provider) {
        DatabaseProvider.POSTGRESQL -> dataSourceFor(postgresqlContainer)
        DatabaseProvider.HSQL -> DriverManagerDataSource("jdbc:hsqldb:mem:test", "sa", "")
    }

    fun createConnectionFactory(provider: DatabaseProvider): ConnectionFactory = when (provider) {
        DatabaseProvider.POSTGRESQL -> ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, postgresqlContainer.host)
                .option(ConnectionFactoryOptions.PORT, postgresqlContainer.firstMappedPort)
                .option(ConnectionFactoryOptions.USER, postgresqlContainer.username)
                .option(ConnectionFactoryOptions.PASSWORD, postgresqlContainer.password)
                .option(ConnectionFactoryOptions.DATABASE, postgresqlContainer.databaseName)
                .build()
        )

        DatabaseProvider.HSQL -> {
            Assumptions.assumeTrue(false)
            error("unreachable")
        }
    }

    private fun dataSourceFor(container: JdbcDatabaseContainer<*>): DataSource =
        DriverManagerDataSource(container.jdbcUrl, container.username, container.password)

    @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
    private class DriverManagerDataSource(
        private val url: String,
        private val defaultUser: String,
        private val defaultPassword: String
    ) : DataSource by unimplemented() {

        override fun getConnection(): Connection =
            getConnection(defaultUser, defaultPassword)

        override fun getConnection(username: String?, password: String?): Connection =
            DriverManager.getConnection(url, username, password)
    }
}
