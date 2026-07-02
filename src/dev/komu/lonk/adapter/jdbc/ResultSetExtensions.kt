package dev.komu.lonk.adapter.jdbc

import dev.komu.lonk.DatabaseException
import java.sql.ResultSetMetaData
import kotlin.reflect.KClass

internal fun ResultSetMetaData.getColumnClass(column: Int): KClass<*> {
    val className = getColumnClassName(column)

    try {
        return Class.forName(className).kotlin
    } catch (e: ClassNotFoundException) {
        throw DatabaseException("Could not find class '$className' specified by ResultSet.", e)
    }
}
