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

        // TODO timeout

        for ((i, value) in query.arguments.withIndex())
            statement.bindArgument(i, value)

        val result = statement.execute().awaitFirst()
        return result.rowsUpdated.awaitFirstOrNull() ?: 0
    }

    override suspend fun <T> doExecuteQuery(processor: ResultAggregator<T>, query: DatabaseQuery): T {
        val statement = connection.createStatement(query.translatedSql)

        for ((i, value) in query.arguments.withIndex())
            statement.bindArgument(i, value)

        statement.execute().collect { result ->
            result.map { row, metadata -> processor.process(R2DBCResultRow(row, metadata)) }.collect { }
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

private fun Statement.bindArgument(index: Int, value: Any?) {
    when {
        value is InputStream -> bind(index, value.readAllBytes()) // value.toBlob())
        value is Reader -> bind(index, value.readText()) // value.toClob())
        value != null -> bind(index, value)
        else -> bindNull(index, Any::class.java)
    }
}

