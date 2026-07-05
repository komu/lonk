package dev.komu.lonk.result

import kotlin.reflect.KClass

/** A single row of a query result. */
public interface ResultRow {

    /** The number of columns in the row. */
    public val columnCount: Int

    /** The valid 0-based indices for columns in the row. */
    public val columnIndices: IntRange
        get() = 0..<columnCount

    /** Returns the values of this row */
    public val values: List<*>
        get() = columnIndices.map { this[it] }

    /** Returns the types of this row */
    public val types: List<KClass<*>>
        get() = columnIndices.map { getColumnClass(it) }

    /** Returns the value of a column using its 0-based index */
    public operator fun get(index: Int): Any?

    /** Returns the value of a column using its name */
    public operator fun get(name: String): Any?

    /** Returns the label of the column at [index]. */
    public fun getColumnLabel(index: Int): String

    /** Returns the Kotlin type of the column at [index]. */
    public fun getColumnClass(index: Int): KClass<*>
}

/** Returns the value of the column at [index], cast to [T]. */
public inline fun <reified T> ResultRow.get(index: Int): T =
    get(index) as T

/** Returns the value of the column named [name], cast to [T]. */
public inline fun <reified T> ResultRow.get(name: String): T =
    get(name) as T

