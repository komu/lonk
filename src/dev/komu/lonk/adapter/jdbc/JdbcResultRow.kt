package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.LonkException
import dev.komu.lonk.result.ResultRow
import java.sql.ResultSet
import kotlin.reflect.KClass

internal class JdbcResultRow(private val resultSet: ResultSet) : ResultRow {

    private val metaData by lazy { resultSet.metaData }

    override val columnCount: Int
        get() = metaData.columnCount

    override fun getColumnLabel(index: Int): String =
        metaData.getColumnLabel(index + 1)

    override fun getColumnClass(index: Int): KClass<*> {
        val className = metaData.getColumnClassName(index + 1)

        try {
            return Class.forName(className).kotlin
        } catch (e: ClassNotFoundException) {
            throw LonkException("Could not find class '$className' specified by ResultSet.", e)
        }
    }

    override fun get(index: Int): Any? =
        resultSet.getObject(index + 1)
}
