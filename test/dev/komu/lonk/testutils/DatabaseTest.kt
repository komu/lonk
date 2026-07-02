package dev.komu.lonk.testutils

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.adapter.jdbc.JdbcConnectionProvider
import dev.komu.lonk.adapter.r2dbc.PlaceholderTranslation
import dev.komu.lonk.adapter.r2dbc.R2dbcConnectionProvider
import dev.komu.lonk.testutils.DatabaseProvider.HSQL
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.ClassTemplate
import org.junit.jupiter.api.extension.*
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.util.stream.Stream
import javax.sql.DataSource

@ClassTemplate
@ExtendWith(DatabaseSourceClassTemplateProvider::class)
internal annotation class DatabaseTest(val provider: DatabaseProvider)

internal enum class DatabaseProvider {
    POSTGRESQL,
    HSQL,
}

internal class DatabaseSourceClassTemplateProvider : ClassTemplateInvocationContextProvider {

    override fun supportsClassTemplate(context: ExtensionContext) = true

    override fun provideClassTemplateInvocationContexts(context: ExtensionContext): Stream<ClassTemplateInvocationContext> {
        val provider = context.requiredTestClass.getAnnotation(DatabaseTest::class.java).provider

        return ConnectivityMode.entries.stream().map { mode ->
            object : ClassTemplateInvocationContext {
                override fun getDisplayName(invocationIndex: Int) = mode.name
                override fun getAdditionalExtensions(): List<Extension> =
                    listOf(DatabaseResolver(provider, mode))
            }
        }
    }
}

internal enum class ConnectivityMode {
    JDBC, R2DBC;

    fun createDatabaseSource(provider: DatabaseProvider) = when (this) {
        JDBC -> JdbcConnectionProvider(TestDatabaseProvider.createDataSource(provider))
        R2DBC -> R2dbcConnectionProvider(TestDatabaseProvider.createConnectionFactory(provider)) {
            placeholderTranslation = PlaceholderTranslation.PostgreSQL
        }
    }
}

private class DatabaseResolver(private val provider: DatabaseProvider, private val mode: ConnectivityMode) :
    ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val type = parameterContext.parameter.type
        return type == DataSource::class.java
                || type == DbConnectionProvider::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val type = parameterContext.parameter.type
        val provider = this.provider

        val dataSource = TestDatabaseProvider.createDataSource(provider)
        return when (type.kotlin) {
            DataSource::class -> dataSource
            DbConnectionProvider::class -> mode.createDatabaseSource(provider)
            else -> error("unsupported type: $type")
        }
    }
}

private object TestDatabaseProvider {

    private val postgresqlContainer by lazy {
        PostgreSQLContainer("postgres:17").also { it.start() }
    }

    fun createDataSource(provider: DatabaseProvider): DataSource = when (provider) {
        POSTGRESQL -> dataSourceFor(postgresqlContainer)
        HSQL -> DriverManagerDataSource("jdbc:hsqldb:mem:test", "sa", "")
    }

    fun createConnectionFactory(provider: DatabaseProvider): ConnectionFactory = when (provider) {
        POSTGRESQL -> ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, postgresqlContainer.host)
                .option(PORT, postgresqlContainer.firstMappedPort)
                .option(USER, postgresqlContainer.username)
                .option(PASSWORD, postgresqlContainer.password)
                .option(DATABASE, postgresqlContainer.databaseName)
                .build()
        )

        HSQL -> {
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
