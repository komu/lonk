package dev.komu.lonk.result

import kotlin.reflect.KClass

internal class MockResultRow(override val values: List<Any>) : ResultRow {

    constructor(vararg values: Any) : this(values.asList())

    override fun get(name: String) = error("no used")

    override val columnCount: Int
        get() = values.size

    override fun getColumnLabel(index: Int): String =
        "column $index"

    override fun getColumnClass(index: Int): KClass<*> =
        values[index]::class

    override fun get(index: Int) = values[index]
}
