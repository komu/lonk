package dev.komu.lonk.testutils

import dev.komu.lonk.DbConnectionProvider
import dev.komu.lonk.adapter.jdbc.JdbcConnectionProvider
import dev.komu.lonk.adapter.r2dbc.PlaceholderTranslation
import dev.komu.lonk.adapter.r2dbc.R2dbcConnectionProvider
import dev.komu.lonk.conversion.TypeConversions
import dev.komu.lonk.testutils.ConnectivityMode.JDBC
import dev.komu.lonk.testutils.ConnectivityMode.R2DBC
import org.junit.jupiter.api.ClassTemplate
import org.junit.jupiter.api.extension.*
import java.util.stream.Stream
import kotlin.reflect.KClass

@ClassTemplate
@ExtendWith(DatabaseSourceClassTemplateProvider::class)
internal annotation class DatabaseTest(
    val provider: DatabaseProvider,
    val conversions: Array<KClass<out TypeConversions>> = [],
    val modes: Array<ConnectivityMode> = [JDBC, R2DBC],
)

internal class DatabaseSourceClassTemplateProvider : ClassTemplateInvocationContextProvider {

    override fun supportsClassTemplate(context: ExtensionContext) = true

    override fun provideClassTemplateInvocationContexts(context: ExtensionContext): Stream<ClassTemplateInvocationContext> {
        val annotation = context.requiredTestClass.getAnnotation(DatabaseTest::class.java)
        val provider = annotation.provider
        val conversions =
            annotation.conversions.map { it.objectInstance ?: error("$it must be an object") }.toMutableList()

        return annotation.modes.asList().stream().map { mode ->
            object : ClassTemplateInvocationContext {
                override fun getDisplayName(invocationIndex: Int) = mode.name
                override fun getAdditionalExtensions() = listOf(
                    DatabaseResolver(
                        provider = provider,
                        mode = mode,
                        conversions = conversions,
                    )
                )
            }
        }
    }
}

internal enum class ConnectivityMode {
    JDBC, R2DBC
}

private class DatabaseResolver(
    private val provider: DatabaseProvider,
    private val mode: ConnectivityMode,
    private val conversions: List<TypeConversions>,
) : ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type == DbConnectionProvider::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        when (mode) {
            JDBC -> JdbcConnectionProvider(TestDataSourceProvider.createDataSource(provider)) {
                conversions {
                    for (conversion in conversions)
                        register(conversion)
                }
            }

            R2DBC -> R2dbcConnectionProvider(TestDataSourceProvider.createConnectionFactory(provider)) {
                placeholderTranslation = PlaceholderTranslation.PostgreSQL

                conversions {
                    for (conversion in conversions)
                        register(conversion)
                }
            }
        }
}

