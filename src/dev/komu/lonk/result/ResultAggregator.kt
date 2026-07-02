package dev.komu.lonk.result

import dev.komu.lonk.instantiation.NamedTypeList
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Callback for processing a whole [ResultSet].
 */
public interface ResultAggregator<T> {
    public fun process(row: ResultRow)
    public fun build(): T
}

public interface ResultRow {
    public operator fun get(index: Int): Any?
    public val columnCount: Int
    public fun getColumnLabel(index: Int): String
    public fun getColumnClass(index: Int): KClass<*>
}

internal fun ResultRow.getTypes(): NamedTypeList {
    val result = NamedTypeList.builder(columnCount)

    for (i in 0..<columnCount)
        result.add(getColumnLabel(i), getColumnClass(i))

    return result.build()
}

public inline fun <reified T> ResultRow.get(index: Int): T =
    get(index) as T
