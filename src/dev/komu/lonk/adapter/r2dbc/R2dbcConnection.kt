package dev.komu.lonk.adapter.r2dbc

import dev.komu.lonk.DatabaseQuery
import dev.komu.lonk.DbConnection
import dev.komu.lonk.instantiation.InstantiatorProvider
import dev.komu.lonk.result.ResultAggregator
import dev.komu.lonk.result.ResultRow
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import java.io.InputStream
import java.io.Reader
import kotlin.reflect.KClass

public class R2dbcConnection internal constructor(
    private val connection: Connection,
    instantiatorProvider: InstantiatorProvider,
    private val placeholderTranslation: PlaceholderTranslation,
) : DbConnection(instantiatorProvider) {

    override suspend fun doUpdate(query: DatabaseQuery): Long {
        val statement = connection.createStatement(query.translatedSql)
        statement.bindFrom(query)

        val result = statement.execute().cancelling(connection).awaitFirst()
        return result.rowsUpdated.cancelling(connection).awaitFirstOrNull() ?: 0
    }

    override suspend fun <T> executeQuery(processor: ResultAggregator<T>, query: DatabaseQuery): T {
        val statement = connection.createStatement(query.translatedSql)

        statement.bindFrom(query)

        statement.execute().cancelling(connection).collect { result ->
            result.map { row, metadata -> processor.process(R2DBCResultRow(row, metadata)) }
                .cancelling(connection)
                .collect { }
        }

        return processor.build()
    }

    private class R2DBCResultRow(private val row: Row, private val metadata: RowMetadata) : ResultRow {

        override val columnCount: Int
            get() = metadata.columnMetadatas.size

        override fun get(index: Int): Any? =
            row.get(index)

        override fun getColumnLabel(index: Int): String =
            metadata.getColumnMetadata(index).name

        override fun getColumnClass(index: Int): KClass<*> =
            metadata.getColumnMetadata(index).javaType.kotlin
    }

    override suspend fun close() {
        connection.close().awaitFirstOrNull()
    }

    override suspend fun commit() {
        connection.commitTransaction().awaitFirstOrNull()
    }

    override suspend fun rollback() {
        connection.rollbackTransaction().awaitFirstOrNull()
    }

    private val DatabaseQuery.translatedSql: String
        get() = when (placeholderTranslation) {
            PlaceholderTranslation.PostgreSQL -> translatePlaceholdersForPostgreSQL(sql)
            PlaceholderTranslation.None -> sql
        }
}

// TODO: this causes race conditions in postgres driver
private fun <T> Publisher<T>.cancelling(connection: Connection): Flux<T> =
    Flux.from(this)
        // TODO this still requires PostgreSQL driver on classpath. modify the code so that it's not required
//     Flux.from(this).doOnCancel {
//        if (connection is PostgresqlConnection) {
//            connection.cancelRequest().subscribe(null) { }
//        }
//    }

private fun Statement.bindFrom(query: DatabaseQuery) {
    for ((i, value) in query.arguments.withIndex())
        bindArgument(i, value)
}

private fun Statement.bindArgument(index: Int, value: Any?) {
    when {
        value is InputStream -> bind(index, value.readAllBytes()) // value.toBlob())
        value is Reader -> bind(index, value.readText()) // value.toClob())
        value != null -> bind(index, value)
        else -> bindNull(index, Any::class.java)
    }
}

