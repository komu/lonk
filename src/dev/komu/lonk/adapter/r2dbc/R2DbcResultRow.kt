package dev.komu.lonk.adapter.r2dbc

import dev.komu.lonk.result.ResultRow
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlin.reflect.KClass

internal class R2DbcResultRow(private val row: Row, private val metadata: RowMetadata) : ResultRow {

    override val columnCount: Int
        get() = metadata.columnMetadatas.size

    override fun get(name: String): Any? =
        row.get(name)

    override fun get(index: Int): Any? =
        row.get(index)

    override fun getColumnLabel(index: Int): String =
        metadata.getColumnMetadata(index).name

    override fun getColumnClass(index: Int): KClass<*> =
        metadata.getColumnMetadata(index).javaType.kotlin
}
