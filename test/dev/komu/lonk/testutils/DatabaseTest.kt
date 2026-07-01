package dev.komu.lonk.testutils

import dev.komu.lonk.DatabaseSource
import dev.komu.lonk.testutils.DatabaseProvider.HSQL
import dev.komu.lonk.testutils.DatabaseProvider.POSTGRESQL
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

@ExtendWith(DatabaseResolver::class)
internal annotation class DatabaseTest(val provider: DatabaseProvider)

internal enum class DatabaseProvider {
    POSTGRESQL,
    HSQL,
}

private class DatabaseResolver : ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        val type = parameterContext.parameter.type
        return type == DataSource::class.java
            || type == DatabaseSource::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val type = parameterContext.parameter.type
        val databaseTest = extensionContext.requiredTestClass.getAnnotation(DatabaseTest::class.java)
            ?: error("Test class is not annotated with @DatabaseTest")
        val provider = databaseTest.provider

        val dataSource = TestDatabaseProvider.createDataSource(provider)
        return when (type.kotlin) {
            DataSource::class -> dataSource
            DatabaseSource::class -> DatabaseSource(dataSource)
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
