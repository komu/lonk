package dev.komu.lonk.jdbc

import dev.komu.lonk.adapter.jdbc.getColumnClass
import dev.komu.lonk.testutils.unimplemented
import java.sql.ResultSetMetaData
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResultSetUtilsTest {

    @Test
    fun `column type resolution`() {
        val metadata = MockResultSetMetaData(
            classNames = listOf("java.lang.String", "[Ljava.lang.String;", "[B")
        )
        assertEquals(String::class, metadata.getColumnClass(1))
        assertEquals(Array<String>::class, metadata.getColumnClass(2))
        assertEquals(ByteArray::class, metadata.getColumnClass(3))
    }

    class MockResultSetMetaData(val classNames: List<String>) : ResultSetMetaData by unimplemented() {
        override fun getColumnCount() = classNames.size
        override fun getColumnClassName(column: Int) = classNames[column - 1]
    }
}
